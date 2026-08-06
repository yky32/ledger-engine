package com.altech.ledger.endpoint.movement;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.ledger.LedgerDto.PageResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.*;
import com.altech.ledger.usecase.movement.MovementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movements")
@RequiredArgsConstructor
public class MovementEndpoint {
    private final MovementUseCase movementUseCase;

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<MovementResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return R.success(movementUseCase.deposit(request));
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<MovementResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return R.success(movementUseCase.withdraw(request));
    }

    @PostMapping("/transfers/in-wallet")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<MovementResponse> transfer(@Valid @RequestBody InWalletTransferRequest request) {
        return R.success(movementUseCase.transfer(request));
    }

    @PutMapping("/{id}/settle")
    public Result<MovementResponse> settle(@PathVariable Long id, @Valid @RequestBody SettleMovementRequest request) {
        return R.success(movementUseCase.settle(id, request));
    }

    @GetMapping("/{id}")
    public Result<MovementResponse> get(@PathVariable Long id) {
        return R.success(movementUseCase.get(id));
    }

    @GetMapping
    public Result<PageResponse<MovementResponse>> list(
        @RequestParam Long walletId,
        @PageableDefault(size = 20, sort = "createDt") Pageable pageable
    ) {
        return R.success(movementUseCase.listByWallet(walletId, pageable));
    }
}
