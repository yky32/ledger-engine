package com.altech.ledger.endpoint.ledger.movement;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.ledger.LedgerMovementOperationUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;

@RestController
@RequestMapping("/ledger-accounts/movements")
@RequiredArgsConstructor
public class LedgerMovementEndpoint {
    private final LedgerMovementOperationUseCase ledgerMovementOperationUseCase;

    @PutMapping("/{id}/statuses")
    public Result<LedgerMovementDtos.Response> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody LedgerMovementDtos.UpdateStatusRequest dto
    ) {
        return R.success(ledgerMovementOperationUseCase.update(id, dto));
    }

    @PutMapping("/{id}/settle")
    public Result<LedgerMovementDtos.Response> settle(@PathVariable Long id) {
        return R.success(ledgerMovementOperationUseCase.settle(id));
    }
}
