package com.altech.ledger.entity.dto.request;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Withdrawal from an originator wallet; optional external target id.
 * Legacy JSON key {@code originatorId} binds via {@link JsonAlias}.
 */
public record CreateLedgerWithdrawalRequestDto(
    @JsonAlias({"originatorId"})
    String originatorWalletId,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
    LedgerMovementMode mode,
    String targetId,
    @Size(max = 150) String movementKey,
    @Size(max = 500) String description
) {
    public CreateLedgerWithdrawalRequestDto {
        if (mode == null) {
            mode = LedgerMovementMode.AUTO;
        }
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
        if (originatorWalletId != null) {
            originatorWalletId = originatorWalletId.trim();
        }
    }

    @JsonIgnore
    public String resolvedOriginatorWalletId() {
        return blankToNull(originatorWalletId);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
