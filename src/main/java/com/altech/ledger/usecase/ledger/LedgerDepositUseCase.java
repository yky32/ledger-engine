package com.altech.ledger.usecase.ledger;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.service.PaymentRailPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.movement.LedgerMovementDtos;

/**
 * Port of the-wallet-ledger LedgerDepositUseCase (bank auto + card session + webhook).
 */
@Service
@RequiredArgsConstructor
public class LedgerDepositUseCase {
    private final LedgerMovementShooter shooter;
    private final PaymentRailPort paymentRailPort;

    @Transactional
    public LedgerMovementDtos.Response execute(LedgerMovementDtos.CreateDepositRequest dto) {
        return shooter.doDeposit(dto);
    }

    /**
     * Card deposit: create external session only (balance applied on webhook).
     */
    public Map<String, String> initiateCardDeposit(Long walletId, String currency, BigDecimal amount,
                                                   Map<String, Object> metadata) {
        String sessionId = paymentRailPort.initiateCardDeposit(walletId, currency, amount, metadata);
        return Map.of(
            "sessionId", sessionId == null ? "" : sessionId,
            "walletId", String.valueOf(walletId),
            "currency", currency,
            "amount", amount.toPlainString());
    }

    /**
     * GrandPay-style deposit callback (flexible payload keys).
     */
    @Transactional
    public LedgerMovementDtos.Response webhookCallback(Map<String, Object> payload) {
        String walletId = first(payload, "targetWalletId", "walletId", "targetId");
        String currency = first(payload, "currency", "ccy");
        if (currency == null) currency = "USD";
        String amountStr = first(payload, "amount", "txnAmount", "value");
        BigDecimal amount = new BigDecimal(amountStr == null ? "0" : amountStr);
        String movementKey = first(payload, "movementKey", "eventId", "txnId", "id");
        return shooter.doDeposit(new LedgerMovementDtos.CreateDepositRequest(
            walletId, currency, amount, LedgerMovementMode.AUTO, null, movementKey,
            "webhook deposit", payload));
    }

    private static String first(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null && !String.valueOf(v).isBlank() && !"null".equals(String.valueOf(v))) {
                return String.valueOf(v);
            }
        }
        return null;
    }
}
