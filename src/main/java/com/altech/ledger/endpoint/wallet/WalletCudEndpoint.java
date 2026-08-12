package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.BatchCreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.response.BatchCreateWalletOnboardResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet <b>CUD</b> API (create / batch create; update-delete when added).
 * <p>
 * Phase-1 onboard: 1 CUST ({@code ownerId}) → 1 Wallet + accounts.
 * Read path lives in {@link WalletQueryEndpoint}.
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletCudEndpoint {
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;

    /** Create one wallet (+ optional extra accounts under settlement). */
    @PostMapping
    public Result<GetWalletOnboardResponseDto> create(@Valid @RequestBody CreateWalletOnboardRequestDto request) {
        return R.success(createWalletOnboardingUseCase.execute(request));
    }

    /** Soft-idempotent CRM bulk onboard (max 1000 per request). */
    @PostMapping("/batch")
    public Result<BatchCreateWalletOnboardResponseDto> createBatch(
        @Valid @RequestBody BatchCreateWalletOnboardRequestDto request
    ) {
        return R.success(createWalletOnboardingUseCase.executeBatch(request));
    }
}
