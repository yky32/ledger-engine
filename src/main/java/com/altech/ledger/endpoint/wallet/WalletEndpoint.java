package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.wallet.*;
import com.altech.ledger.usecase.wallet.WalletOnboardingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletEndpoint {
    private final WalletOnboardingUseCase onboardingUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<WalletOnboardResponse> onboard(@Valid @RequestBody OnboardWalletRequest request) {
        return R.success(onboardingUseCase.onboard(request));
    }

    @GetMapping("/{ownerId}/{currency}")
    public Result<WalletOnboardResponse> get(@PathVariable String ownerId, @PathVariable String currency) {
        return R.success(onboardingUseCase.getByOwner(ownerId, currency));
    }

    @GetMapping
    public Result<List<WalletOnboardResponse>> list(@RequestParam String ownerId) {
        return R.success(onboardingUseCase.listByOwner(ownerId));
    }

    /** Bulk import from client CRM / legacy membership system during go-live. */
    @PostMapping("/batch")
    public Result<BatchOnboardWalletResponse> onboardBatch(@Valid @RequestBody BatchOnboardWalletRequest request) {
        return R.success(onboardingUseCase.onboardBatch(request));
    }
}
