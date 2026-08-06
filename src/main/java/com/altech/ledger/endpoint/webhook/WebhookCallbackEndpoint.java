package com.altech.ledger.endpoint.webhook;

import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.entity.dto.parity.ParityDtos.WalletWithBalancesResponse;
import com.altech.ledger.usecase.ledger.LedgerDepositUseCase;
import com.altech.ledger.usecase.setup.WalletSetupUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookCallbackEndpoint {
    private final WalletSetupUseCase walletSetupUseCase;
    private final LedgerDepositUseCase depositUseCase;

    @PostMapping("/ledger-wallets/{walletId}/activations")
    public WalletWithBalancesResponse walletActivation(
        @PathVariable Long walletId,
        @RequestBody(required = false) Map<String, Object> payload
    ) {
        return walletSetupUseCase.markActive(walletId);
    }

    @PostMapping("/ledger-wallets/movements/deposits")
    public MovementResponse depositCallback(@RequestBody Map<String, Object> payload) {
        return depositUseCase.webhookCallback(payload);
    }
}
