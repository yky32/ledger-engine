package com.altech.ledger.endpoint.ledger.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.usecase.MyWalletUseCase;
import com.altech.ledger.usecase.account.WalletAccountBalanceUseCase;
import com.altech.ledger.usecase.setup.WalletSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ledger-wallets")
@RequiredArgsConstructor
public class LedgerWalletEndpoint {
    private final WalletSetupUseCase walletSetupUseCase;
    private final WalletAccountBalanceUseCase balanceUseCase;
    private final MyWalletUseCase myWalletUseCase;

    @PostMapping
    public Result<LedgerWalletDtos.WithBalancesResponse> create(@Valid @RequestBody LedgerWalletDtos.CreateRequest dto) {
        return R.success(walletSetupUseCase.create(dto));
    }

    /** Convenience: create main account + wallet (legacy associatedWithAccountsCreation). */
    @PostMapping("/full")
    public Result<LedgerWalletDtos.WithBalancesResponse> createFull(
        @RequestParam String ownerId,
        @RequestParam String currency,
        @RequestParam(required = false) String extIdentifier,
        @RequestParam(required = false) String extType,
        @RequestParam(required = false) List<String> extraCurrencies
    ) {
        return R.success(walletSetupUseCase.createFull(ownerId, currency, extraCurrencies, extIdentifier, extType));
    }

    @PostMapping("/{id}/activations")
    public Result<LedgerWalletDtos.WithBalancesResponse> activate(
        @PathVariable Long id,
        @RequestBody(required = false) LedgerWalletDtos.ActivationRequest dto
    ) {
        return R.success(walletSetupUseCase.activate(id, dto == null
            ? new LedgerWalletDtos.ActivationRequest(null, null) : dto));
    }

    @PutMapping("/{id}/statuses")
    public Result<LedgerWalletDtos.WithBalancesResponse> updateStatuses(
        @PathVariable Long id,
        @Valid @RequestBody LedgerWalletDtos.UpdateRequest putDto
    ) {
        return R.success(walletSetupUseCase.update(id, putDto));
    }

    @GetMapping("/{id}")
    public Result<LedgerWalletDtos.WithBalancesResponse> getOne(
        @PathVariable Long id,
        @RequestParam(required = false) String fxTarget
    ) {
        return R.success(balanceUseCase.getOne(id, fxTarget));
    }

    @GetMapping
    public Result<List<LedgerWalletDtos.WithBalancesResponse>> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) String fxTarget
    ) {
        return R.success(balanceUseCase.getAll(pageable, fxTarget));
    }

    @GetMapping("/my-wallets")
    public Result<List<LedgerWalletDtos.WithBalancesResponse>> myWallets(
        @RequestParam String ownerId,
        @RequestParam(required = false) String fxTarget
    ) {
        if (fxTarget == null || fxTarget.isBlank()) {
            return R.success(myWalletUseCase.execute(ownerId));
        }
        return R.success(balanceUseCase.myWallets(ownerId, fxTarget));
    }

    @GetMapping("/ext/{type}/{id}")
    public Result<LedgerWalletDtos.WithBalancesResponse> getByExtIdentifier(
        @PathVariable String type,
        @PathVariable String id
    ) {
        return R.success(balanceUseCase.getByExtIdentifier(id, type));
    }
}
