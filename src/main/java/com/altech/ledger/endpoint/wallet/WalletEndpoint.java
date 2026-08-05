package com.altech.ledger.endpoint.wallet;

import com.altech.ledger.entity.dto.wallet.*;
import com.altech.ledger.usecase.wallet.WalletOnboardingUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletEndpoint {
    private final WalletOnboardingUseCase onboardingUseCase;

    public WalletEndpoint(WalletOnboardingUseCase onboardingUseCase) {
        this.onboardingUseCase = onboardingUseCase;
    }

    @PostMapping
    public ResponseEntity<WalletOnboardResponse> onboard(@Valid @RequestBody OnboardWalletRequest request) {
        WalletOnboardResponse response = onboardingUseCase.onboard(request);
        return ResponseEntity.created(URI.create("/wallets/" + response.walletId())).body(response);
    }

    @GetMapping("/{ownerId}/{currency}")
    public WalletOnboardResponse get(@PathVariable String ownerId, @PathVariable String currency) {
        return onboardingUseCase.getByOwner(ownerId, currency);
    }

    @GetMapping
    public List<WalletOnboardResponse> list(@RequestParam String ownerId) {
        return onboardingUseCase.listByOwner(ownerId);
    }

    /** Bulk import from client CRM / legacy membership system during go-live. */
    @PostMapping("/batch")
    public BatchOnboardWalletResponse onboardBatch(@Valid @RequestBody BatchOnboardWalletRequest request) {
        return onboardingUseCase.onboardBatch(request);
    }
}
