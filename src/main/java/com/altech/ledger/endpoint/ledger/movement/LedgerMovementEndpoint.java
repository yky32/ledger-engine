package com.altech.ledger.endpoint.ledger.movement;

import com.altech.ledger.usecase.ledger.LedgerMovementOperationUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.movement.LedgerMovementDtos;

@RestController
@RequestMapping("/ledger-accounts/movements")
@RequiredArgsConstructor
public class LedgerMovementEndpoint {
    private final LedgerMovementOperationUseCase operationUseCase;

    @PutMapping("/{id}/statuses")
    public LedgerMovementDtos.Response updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody LedgerMovementDtos.UpdateStatusRequest dto
    ) {
        return operationUseCase.update(id, dto);
    }

    @PutMapping("/{id}/settle")
    public LedgerMovementDtos.Response settle(@PathVariable Long id) {
        return operationUseCase.settle(id);
    }
}
