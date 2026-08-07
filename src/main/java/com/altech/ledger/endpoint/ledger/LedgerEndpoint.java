package com.altech.ledger.endpoint.ledger;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.usecase.ledger.CreateLedgerAccountUseCase;
import com.altech.ledger.usecase.ledger.QueryLedgerAccountUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LedgerEndpoint {
    private final CreateLedgerAccountUseCase createLedgerAccountUseCase;
    private final QueryLedgerAccountUseCase queryLedgerAccountUseCase;

    @PostMapping("/accounts")
    public Result<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return R.success(createLedgerAccountUseCase.execute(request));
    }

    @GetMapping("/accounts/{id}")
    public Result<AccountResponse> getAccount(@PathVariable Long id) {
        return R.success(queryLedgerAccountUseCase.one(id));
    }

    @GetMapping("/accounts/{id}/balance")
    public Result<BalanceResponse> getBalance(@PathVariable Long id) {
        return R.success(queryLedgerAccountUseCase.balance(id));
    }
}
