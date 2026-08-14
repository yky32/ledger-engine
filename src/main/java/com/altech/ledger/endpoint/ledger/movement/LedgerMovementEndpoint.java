package com.altech.ledger.endpoint.ledger.movement;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.ledger.LedgerMovementOperationUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.request.UpdateLedgerMovementStatusRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

/**
 * @deprecated Product: {@code /movements} and {@code /wallets/{ownerId}/movements}.
 * Still served for in-cluster compatibility; do not use for new LedgeRX product work.
 * @see docs/TECH_DEBT.md TD-API-001
 */
@Deprecated(since = "coa-profile", forRemoval = false)
@RestController
@RequestMapping("/ledger-accounts/movements")
@RequiredArgsConstructor
public class LedgerMovementEndpoint {
    private final LedgerMovementOperationUseCase ledgerMovementOperationUseCase;

    @PutMapping("/{id}/statuses")
    public Result<GetLedgerMovementResponseDto> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLedgerMovementStatusRequestDto dto
    ) {
        return R.success(ledgerMovementOperationUseCase.update(id, dto));
    }

    @PutMapping("/{id}/settle")
    public Result<GetLedgerMovementResponseDto> settle(@PathVariable Long id) {
        return R.success(ledgerMovementOperationUseCase.settle(id));
    }
}
