package com.altech.ledger.endpoint.usecase.withdrawal;

import com.altech.ledger.entity.dto.parity.ParityDtos.CreateWithdrawalRequest;
import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class WithdrawalEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    public WithdrawalEndpoint(LedgerMovementPipelineUseCase pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/ledger/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse withdraw(@Valid @RequestBody CreateWithdrawalRequest dto) {
        return pipeline.withdraw(dto);
    }
}
