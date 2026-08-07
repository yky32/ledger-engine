package com.altech.ledger.endpoint.usecase.withdrawal;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;

@RestController
@RequiredArgsConstructor
public class WithdrawalEndpoint {
    private final LedgerMovementPipelineUseCase ledgerMovementPipelineUseCase;

    @PostMapping("/ledger/withdrawals")
    public Result<LedgerMovementDtos.Response> withdraw(@Valid @RequestBody LedgerMovementDtos.CreateWithdrawalRequest dto) {
        return R.success(ledgerMovementPipelineUseCase.withdraw(dto));
    }
}
