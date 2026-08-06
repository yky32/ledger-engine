package com.altech.ledger.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Stub for GrandPay / card acquirer (the-wallet-ledger external rail).
 */
public interface PaymentRailPort {
    /**
     * @return external checkout/session id, or null if not applicable
     */
    String initiateCardDeposit(Long walletId, String currency, BigDecimal amount, Map<String, Object> metadata);

    final class NoOpPaymentRailPort implements PaymentRailPort {
        @Override
        public String initiateCardDeposit(Long walletId, String currency, BigDecimal amount,
                                          Map<String, Object> metadata) {
            return "stub-session-" + walletId + "-" + System.currentTimeMillis();
        }
    }
}
