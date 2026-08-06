package com.altech.ledger.endpoint.webhook;

import com.altech.ledger.usecase.ledger.LedgerDepositUseCase;
import com.altech.ledger.usecase.setup.WalletSetupUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.movement.LedgerMovementDtos;
import com.altech.ledger.entity.dto.wallet.LedgerWalletDtos;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookCallbackEndpoint {
    private final WalletSetupUseCase walletSetupUseCase;
    private final LedgerDepositUseCase depositUseCase;

    @PostMapping("/ledger-wallets/{walletId}/activations")
    public LedgerWalletDtos.WithBalancesResponse walletActivation(
        @PathVariable Long walletId,
        @RequestBody(required = false) Map<String, Object> payload
    ) {
        return walletSetupUseCase.markActive(walletId);
    }

    @PostMapping("/ledger-wallets/movements/deposits")
    public LedgerMovementDtos.Response depositCallback(@RequestBody Map<String, Object> payload) {
        return depositUseCase.webhookCallback(payload);
    }
}
