package com.altech.ledger.onboarding;

import com.altech.ledger.api.LedgerDtos.AccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletOnboardingController {
    private final WalletOnboardingService onboardingService;

    public WalletOnboardingController(WalletOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> onboard(@Valid @RequestBody OnboardWalletRequest request) {
        AccountResponse response = onboardingService.onboard(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + response.id())).body(response);
    }

    /** Bulk import from client CRM / legacy membership system during go-live. */
    @PostMapping("/batch")
    public BatchOnboardWalletResponse onboardBatch(@Valid @RequestBody BatchOnboardWalletRequest request) {
        return onboardingService.onboardBatch(request);
    }
}
