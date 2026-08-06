package com.altech.ledger.endpoint.ledger;

import com.altech.ledger.entity.dto.ledger.LedgerDto.*;
import com.altech.ledger.usecase.ledger.LedgerUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
public class LedgerEndpoint {
    private final LedgerUseCase useCase;

    public LedgerEndpoint(LedgerUseCase useCase) {
        this.useCase = useCase;
    }

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

    @GetMapping("/accounts/{id}/entries")
    public PageResponse<EntryResponse> getEntries(
        @PathVariable Long id,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return useCase.getEntries(id, pageable);
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> post(@Valid @RequestBody PostTransactionRequest request) {
        return postingResponse(useCase.post(request));
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse getTransaction(@PathVariable UUID id) {
        return useCase.getTransaction(id);
    }

    @PostMapping("/transactions/{id}/reversal")
    public ResponseEntity<TransactionResponse> reverse(
        @PathVariable UUID id,
        @Valid @RequestBody ReversalRequest request
    ) {
        return postingResponse(useCase.reverse(id, request));
    }

    private ResponseEntity<TransactionResponse> postingResponse(LedgerUseCase.PostingResult result) {
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/transactions/" + result.transaction().id()))
                .body(result.transaction());
        }
        return ResponseEntity.ok(result.transaction());
    }
}
