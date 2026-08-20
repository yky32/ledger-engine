package com.altech.ledger.usecase.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.posting.PostingCommand;
import com.altech.ledger.entity.dto.request.CreateLedgerDepositRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Deposit execution via central {@link ApplyPostingUseCase}.
 */
@Component
@RequiredArgsConstructor
public class LedgerDepositUseCase {
    private final ApplyPostingUseCase applyPostingUseCase;
    private final WalletService walletService;

    @Transactional
    public GetLedgerMovementResponseDto execute(CreateLedgerDepositRequestDto dto) {
        var wallet = walletService.resolve(dto.resolvedTargetWalletId());
        return applyPostingUseCase.execute(PostingCommand.deposit(
            wallet.getId(),
            dto.amount(),
            dto.currency(),
            dto.movementKey(),
            dto.description(),
            dto.mode() == null ? LedgerMovementMode.AUTO : dto.mode()
        ));
    }

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
    public GetLedgerMovementResponseDto executeWebhook(Map<String, Object> payload) {
        String walletId = _first(payload, "targetWalletId", "walletId", "targetId");
        String currencyCode = _first(payload, "currency", "ccy");
        Currency currency = Currency.get(currencyCode == null ? "USD" : currencyCode);
        String amountStr = _first(payload, "amount", "txnAmount", "value");
        BigDecimal amount = new BigDecimal(amountStr == null ? "0" : amountStr);
        String movementKey = _first(payload, "movementKey", "eventId", "txnId", "id");
        var wallet = walletService.resolve(walletId);
        return applyPostingUseCase.execute(PostingCommand.deposit(
            wallet.getId(), amount, currency, movementKey, "webhook deposit", LedgerMovementMode.AUTO));
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
