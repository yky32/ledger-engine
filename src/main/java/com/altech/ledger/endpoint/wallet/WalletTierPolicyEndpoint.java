package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.UpdateWalletTierPolicyRequestDto;
import com.altech.ledger.entity.dto.response.GetWalletTierPolicyResponseDto;
import com.altech.ledger.usecase.wallet.WalletTierPolicyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet membership bands (watched COA book + upgrade/downgrade amounts).
 */
@RestController
@RequestMapping("/wallet-tier-policies")
@RequiredArgsConstructor
public class WalletTierPolicyEndpoint {
    private final WalletTierPolicyUseCase walletTierPolicyUseCase;

    @GetMapping
    public Result<GetWalletTierPolicyResponseDto> get() {
        return R.success(walletTierPolicyUseCase.getOrCreate());
    }

    @PutMapping
    public Result<GetWalletTierPolicyResponseDto> update(@Valid @RequestBody UpdateWalletTierPolicyRequestDto body) {
        return R.success(walletTierPolicyUseCase.update(body));
    }
}
