package com.altech.ledger.endpoint.usecase.withdrawal;

import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.movement.LedgerMovementDtos;

@RestController
@RequiredArgsConstructor
public class WithdrawalEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    @PostMapping("/ledger/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerMovementDtos.Response withdraw(@Valid @RequestBody LedgerMovementDtos.CreateWithdrawalRequest dto) {
        return pipeline.withdraw(dto);
    }
}
