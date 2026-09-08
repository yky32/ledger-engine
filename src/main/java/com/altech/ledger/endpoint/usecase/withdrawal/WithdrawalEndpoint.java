package com.altech.ledger.endpoint.usecase.withdrawal;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.request.CreateLedgerWithdrawalRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

/**
 * Wallet-id rail API (legacy parity with the-wallet-ledger).
 *
 * @deprecated Prefer {@code POST /movements/withdrawals}.
 */
@Deprecated(forRemoval = false)
@RestController
@RequiredArgsConstructor
public class WithdrawalEndpoint {
    private final LedgerMovementPipelineUseCase ledgerMovementPipelineUseCase;

    @PostMapping("/ledger/withdrawals")
    public Result<GetLedgerMovementResponseDto> withdraw(@Valid @RequestBody CreateLedgerWithdrawalRequestDto dto) {
        return R.success(ledgerMovementPipelineUseCase.withdraw(dto));
    }
}
