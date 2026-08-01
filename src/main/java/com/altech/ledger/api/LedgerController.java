package com.altech.ledger.api;

import com.altech.ledger.api.LedgerDtos.*;
import com.altech.ledger.application.LedgerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LedgerController {
    private final LedgerService service;

    public LedgerController(LedgerService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = service.createAccount(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse getAccount(@PathVariable UUID id) {
        return service.getAccount(id);
    }

    @GetMapping("/accounts/{id}/balance")
    public BalanceResponse getBalance(@PathVariable UUID id) {
        return service.getBalance(id);
    }

    @GetMapping("/accounts/{id}/entries")
    public PageResponse<EntryResponse> getEntries(
        @PathVariable UUID id,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return service.getEntries(id, pageable);
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> post(@Valid @RequestBody PostTransactionRequest request) {
        return postingResponse(service.post(request));
    }

    @GetMapping("/transactions/{id}")
    public TransactionResponse getTransaction(@PathVariable UUID id) {
        return service.getTransaction(id);
    }

    @PostMapping("/transactions/{id}/reversal")
    public ResponseEntity<TransactionResponse> reverse(
        @PathVariable UUID id,
        @Valid @RequestBody ReversalRequest request
    ) {
        return postingResponse(service.reverse(id, request));
    }

    private ResponseEntity<TransactionResponse> postingResponse(LedgerService.PostingResult result) {
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/transactions/" + result.transaction().id()))
                .body(result.transaction());
        }
        return ResponseEntity.ok(result.transaction());
    }
}
