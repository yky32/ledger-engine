package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.request.BatchCreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.request.CreateWalletOnboardRequestDto;
import com.altech.ledger.entity.dto.response.BatchCreateWalletOnboardResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.usecase.wallet.CreateWalletOnboardingUseCase;
import com.altech.ledger.usecase.wallet.QueryWalletUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Phase-1 wallet onboarding (TGT-style thin endpoint → use case → {@code R.success}).
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletEndpoint {
    private final CreateWalletOnboardingUseCase createWalletOnboardingUseCase;
    private final QueryWalletUseCase queryWalletUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<GetWalletOnboardResponseDto> create(@Valid @RequestBody CreateWalletOnboardRequestDto request) {
        return R.success(createWalletOnboardingUseCase.execute(request));
    }

    @PostMapping("/batch")
    public Result<BatchCreateWalletOnboardResponseDto> createBatch(
        @Valid @RequestBody BatchCreateWalletOnboardRequestDto request
    ) {
        return R.success(createWalletOnboardingUseCase.executeBatch(request));
    }

    @GetMapping("/{ownerId}/{currency}")
    public Result<GetWalletOnboardResponseDto> get(@PathVariable String ownerId, @PathVariable String currency) {
        return R.success(queryWalletUseCase.get(ownerId, currency));
    }

    @GetMapping
    public Result<List<GetWalletOnboardResponseDto>> list(@RequestParam String ownerId) {
        return R.success(queryWalletUseCase.list(ownerId));
    }
}
