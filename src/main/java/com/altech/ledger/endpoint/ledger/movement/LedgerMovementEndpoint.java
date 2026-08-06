package com.altech.ledger.endpoint.ledger.movement;

import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.entity.dto.parity.ParityDtos.UpdateMovementStatusRequest;
import com.altech.ledger.usecase.ledger.LedgerMovementOperationUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ledger-accounts/movements")
@RequiredArgsConstructor
public class LedgerMovementEndpoint {
    private final LedgerMovementOperationUseCase operationUseCase;

    @PutMapping("/{id}/statuses")
    public MovementResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateMovementStatusRequest dto
    ) {
        return operationUseCase.update(id, dto);
    }

    @PutMapping("/{id}/settle")
    public MovementResponse settle(@PathVariable Long id) {
        return operationUseCase.settle(id);
    }
}
