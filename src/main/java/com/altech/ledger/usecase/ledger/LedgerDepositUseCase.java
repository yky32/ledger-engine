package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Deposit execution: bank / AUTO movement + optional webhook callback.
 */
@Component
@RequiredArgsConstructor
public class LedgerDepositUseCase {
    private final LedgerMovementShooter shooter;

    @Transactional
    public LedgerMovementDtos.Response execute(LedgerMovementDtos.CreateDepositRequest dto) {
        return shooter.doDeposit(dto);
    }

    /**
     * Card-style session placeholder (no external payment rail).
     * Balance is applied when {@link #executeWebhook} (or a normal deposit) is called.
     */
    public Map<String, String> executeCardSession(Long walletId, String currency, BigDecimal amount,
                                                  Map<String, Object> metadata) {
        String sessionId = "session-" + walletId + "-" + UUID.randomUUID();
        return Map.of(
            "sessionId", sessionId,
            "walletId", String.valueOf(walletId),
            "currency", currency,
            "amount", amount.toPlainString());
    }

    /**
     * External deposit callback (flexible payload keys).
     */
    @Transactional
    public LedgerMovementDtos.Response executeWebhook(Map<String, Object> payload) {
        String walletId = _first(payload, "targetWalletId", "walletId", "targetId");
        String currency = _first(payload, "currency", "ccy");
        if (currency == null) {
            currency = "USD";
        }
        String amountStr = _first(payload, "amount", "txnAmount", "value");
        BigDecimal amount = new BigDecimal(amountStr == null ? "0" : amountStr);
        String movementKey = _first(payload, "movementKey", "eventId", "txnId", "id");
        return shooter.doDeposit(new LedgerMovementDtos.CreateDepositRequest(
            walletId, currency, amount, LedgerMovementMode.AUTO, null, movementKey,
            "webhook deposit", payload));
    }

    private static String _first(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null && !String.valueOf(v).isBlank() && !"null".equals(String.valueOf(v))) {
                return String.valueOf(v);
            }
        }
        return null;
    }
}
