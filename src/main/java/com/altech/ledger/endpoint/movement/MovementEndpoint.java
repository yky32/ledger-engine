package com.altech.ledger.endpoint.movement;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.ledger.LedgerDto.PageResponse;
import com.altech.ledger.entity.dto.movement.MovementDto.*;
import com.altech.ledger.usecase.movement.CreateDepositUseCase;
import com.altech.ledger.usecase.movement.CreateInWalletTransferUseCase;
import com.altech.ledger.usecase.movement.CreateWithdrawalUseCase;
import com.altech.ledger.usecase.movement.QueryMovementUseCase;
import com.altech.ledger.usecase.movement.SettleMovementUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movements")
@RequiredArgsConstructor
public class MovementEndpoint {
    private final CreateDepositUseCase createDepositUseCase;
    private final CreateWithdrawalUseCase createWithdrawalUseCase;
    private final CreateInWalletTransferUseCase createInWalletTransferUseCase;
    private final SettleMovementUseCase settleMovementUseCase;
    private final QueryMovementUseCase queryMovementUseCase;

    @PostMapping("/deposits")
    public Result<MovementResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return R.success(createDepositUseCase.execute(request));
    }

    @PostMapping("/withdrawals")
    public Result<MovementResponse> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        return R.success(createWithdrawalUseCase.execute(request));
    }

    @PostMapping("/transfers/in-wallet")
    public Result<MovementResponse> transfer(@Valid @RequestBody InWalletTransferRequest request) {
        return R.success(createInWalletTransferUseCase.execute(request));
    }

    @PutMapping("/{id}/settle")
    public Result<MovementResponse> settle(@PathVariable Long id, @Valid @RequestBody SettleMovementRequest request) {
        return R.success(settleMovementUseCase.execute(id, request));
    }

    @GetMapping("/{id}")
    public Result<MovementResponse> get(@PathVariable Long id) {
        return R.success(queryMovementUseCase.one(id));
    }

    @GetMapping
    public Result<PageResponse<MovementResponse>> list(
        @RequestParam Long walletId,
        @PageableDefault(size = 20, sort = "createDt") Pageable pageable
    ) {
        return R.success(queryMovementUseCase.listByWallet(walletId, pageable));
    }
}
