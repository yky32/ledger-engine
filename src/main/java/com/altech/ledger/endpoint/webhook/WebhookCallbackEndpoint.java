package com.altech.ledger.endpoint.webhook;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.ledger.LedgerDepositUseCase;
import com.altech.ledger.usecase.wallet.ActivateWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class WebhookCallbackEndpoint {
    private final ActivateWalletUseCase activateWalletUseCase;
    private final LedgerDepositUseCase ledgerDepositUseCase;

    @PostMapping("/ledger-wallets/{walletId}/activations")
    public Result<GetLedgerWalletResponseDto> walletActivation(
        @PathVariable Long walletId,
        @RequestBody(required = false) Map<String, Object> payload
    ) {
        return R.success(activateWalletUseCase.execute(walletId));
    }

    @PostMapping("/ledger-wallets/movements/deposits")
    public Result<GetLedgerMovementResponseDto> depositCallback(@RequestBody Map<String, Object> payload) {
        return R.success(ledgerDepositUseCase.executeWebhook(payload));
    }
}
