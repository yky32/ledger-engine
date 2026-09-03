package com.altech.ledger.entity.json_context;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * One membership band. {@code upgradeAt} inclusive; {@code downgradeBelow} exclusive hysteresis.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WalletTierBand(
    String code,
    BigDecimal upgradeAt,
    BigDecimal downgradeBelow
) {
    public WalletTierBand {
        if (code != null) {
            code = code.trim().toUpperCase();
            if (code.isEmpty()) {
                code = null;
            }
        }
    }
}
