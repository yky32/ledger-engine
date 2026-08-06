package com.altech.ledger.endpoint.ledger;

import com.altech.ledger.entity.dto.ledger.LedgerDto.AccountResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.BalanceResponse;
import com.altech.ledger.entity.dto.ledger.LedgerDto.CreateAccountRequest;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class LedgerEndpoint {
    private final LedgerUseCase useCase;

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = useCase.createAccount(request);
        return ResponseEntity.created(URI.create("/accounts/" + response.id())).body(response);
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return useCase.getAccount(id);
    }

    @GetMapping("/accounts/{id}/balance")
    public BalanceResponse getBalance(@PathVariable Long id) {
        return useCase.getBalance(id);
    }
}
