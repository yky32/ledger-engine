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
import java.util.Map;

/**
 * Deposit into a target wallet (id or alias); optional originator and free-form detail map.
 * Legacy JSON key {@code targetId} binds via {@link JsonAlias}.
 */
public record CreateLedgerDepositRequestDto(
    @JsonAlias({"targetId"})
    String targetWalletId,
    @NotBlank @Pattern(regexp = "[A-Z]{2,4}") String currency,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
    LedgerMovementMode mode,
    String originatorId,
    @Size(max = 150) String movementKey,
    @Size(max = 500) String description,
    Map<String, Object> detail
) {
    public CreateLedgerDepositRequestDto {
        if (mode == null) {
            mode = LedgerMovementMode.AUTO;
        }
        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
        if (targetWalletId != null) {
            targetWalletId = targetWalletId.trim();
        }
    }

    @JsonIgnore
    public String resolvedTargetWalletId() {
        return blankToNull(targetWalletId);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
