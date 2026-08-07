package com.altech.ledger.endpoint.usecase.withdrawal;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;

@RestController
@RequiredArgsConstructor
public class WithdrawalEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    @PostMapping("/ledger/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<LedgerMovementDtos.Response> withdraw(@Valid @RequestBody LedgerMovementDtos.CreateWithdrawalRequest dto) {
        return R.success(pipeline.withdraw(dto));
    }
}
