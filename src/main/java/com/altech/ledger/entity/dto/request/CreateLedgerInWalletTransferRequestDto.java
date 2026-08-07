package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Transfer between two wallet ids (same currency).
 */
public record CreateLedgerInWalletTransferRequestDto(
    @NotBlank String fromWalletId,
    @NotBlank String toWalletId,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
    LedgerMovementMode mode,
    @Size(max = 150) String movementKey,
    @Size(max = 500) String description
) {
    public CreateLedgerInWalletTransferRequestDto {
        if (mode == null) {
            mode = LedgerMovementMode.AUTO;
        }
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
    }
}
