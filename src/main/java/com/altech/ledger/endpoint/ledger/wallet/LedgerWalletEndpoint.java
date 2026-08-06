package com.altech.ledger.endpoint.ledger.wallet;

import com.altech.ledger.entity.dto.wallet.LedgerWalletDtos;
import com.altech.ledger.usecase.MyWalletUseCase;
import com.altech.ledger.usecase.account.WalletAccountBalanceUseCase;
import com.altech.ledger.usecase.setup.WalletSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerWalletDtos.WithBalancesResponse create(@Valid @RequestBody LedgerWalletDtos.CreateRequest dto) {
        return walletSetupUseCase.create(dto);
    }

    /** Convenience: create main account + wallet (legacy associatedWithAccountsCreation). */
    @PostMapping("/full")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerWalletDtos.WithBalancesResponse createFull(
        @RequestParam String ownerId,
        @RequestParam String currency,
        @RequestParam(required = false) String extIdentifier,
        @RequestParam(required = false) String extType,
        @RequestParam(required = false) List<String> extraCurrencies
    ) {
        return walletSetupUseCase.createFull(ownerId, currency, extraCurrencies, extIdentifier, extType);
    }

    @PostMapping("/{id}/activations")
    public LedgerWalletDtos.WithBalancesResponse activate(
        @PathVariable Long id,
        @RequestBody(required = false) LedgerWalletDtos.ActivationRequest dto
    ) {
        return walletSetupUseCase.activate(id, dto == null
            ? new LedgerWalletDtos.ActivationRequest(null, null) : dto);
    }

    @PutMapping("/{id}/statuses")
    public LedgerWalletDtos.WithBalancesResponse updateStatuses(
        @PathVariable Long id,
        @Valid @RequestBody LedgerWalletDtos.UpdateRequest putDto
    ) {
        return walletSetupUseCase.update(id, putDto);
    }

    @GetMapping("/{id}")
    public LedgerWalletDtos.WithBalancesResponse getOne(
        @PathVariable Long id,
        @RequestParam(required = false) String fxTarget
    ) {
        return balanceUseCase.getOne(id, fxTarget);
    }

    @GetMapping
    public Page<LedgerWalletDtos.WithBalancesResponse> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) String fxTarget
    ) {
        return balanceUseCase.getAll(pageable, fxTarget);
    }

    @GetMapping("/my-wallets")
    public List<LedgerWalletDtos.WithBalancesResponse> myWallets(
        @RequestParam String ownerId,
        @RequestParam(required = false) String fxTarget
    ) {
        if (fxTarget == null || fxTarget.isBlank()) {
            return myWalletUseCase.execute(ownerId);
        }
        return balanceUseCase.myWallets(ownerId, fxTarget);
    }

    @GetMapping("/ext/{type}/{id}")
    public LedgerWalletDtos.WithBalancesResponse getByExtIdentifier(
        @PathVariable String type,
        @PathVariable String id
    ) {
        return balanceUseCase.getByExtIdentifier(id, type);
    }
}
