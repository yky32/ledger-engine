package com.altech.ledger.endpoint.movement;

import com.altech.ledger.entity.dto.ledger.LedgerDto.PageResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.*;
import com.altech.ledger.usecase.movement.MovementUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/movements")
@RequiredArgsConstructor
public class MovementEndpoint {
    private final MovementUseCase movementUseCase;

    @PostMapping("/deposits")
    public ResponseEntity<MovementResponse> deposit(@Valid @RequestBody DepositRequest request) {
        MovementResponse response = movementUseCase.deposit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/movements/" + response.id()))
            .body(response);
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<MovementResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        MovementResponse response = movementUseCase.withdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/movements/" + response.id()))
            .body(response);
    }

    @PostMapping("/transfers/in-wallet")
    public ResponseEntity<MovementResponse> transfer(@Valid @RequestBody InWalletTransferRequest request) {
        MovementResponse response = movementUseCase.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/movements/" + response.id()))
            .body(response);
    }

    @PutMapping("/{id}/settle")
    public MovementResponse settle(@PathVariable Long id, @Valid @RequestBody SettleMovementRequest request) {
        return movementUseCase.settle(id, request);
    }

    @GetMapping("/{id}")
    public MovementResponse get(@PathVariable Long id) {
        return movementUseCase.get(id);
    }

    @GetMapping
    public PageResponse<MovementResponse> list(
        @RequestParam Long walletId,
        @PageableDefault(size = 20, sort = "createDt") Pageable pageable
    ) {
        return movementUseCase.listByWallet(walletId, pageable);
    }
}
