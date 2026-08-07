package com.altech.ledger.endpoint.ledger.wallet;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.usecase.account.QueryWalletBalanceUseCase;
import com.altech.ledger.usecase.wallet.ActivateWalletUseCase;
import com.altech.ledger.usecase.wallet.CreateWalletUseCase;
import com.altech.ledger.usecase.wallet.QueryMyWalletUseCase;
import com.altech.ledger.usecase.wallet.UpdateWalletUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ledger-wallets")
@RequiredArgsConstructor
public class LedgerWalletEndpoint {
    private final CreateWalletUseCase createWalletUseCase;
    private final ActivateWalletUseCase activateWalletUseCase;
    private final UpdateWalletUseCase updateWalletUseCase;
    private final QueryWalletBalanceUseCase queryWalletBalanceUseCase;
    private final QueryMyWalletUseCase queryMyWalletUseCase;

    @PostMapping
    public Result<LedgerWalletDtos.WithBalancesResponse> create(@Valid @RequestBody LedgerWalletDtos.CreateRequest dto) {
        return R.success(createWalletUseCase.execute(dto));
    }

    @PostMapping("/full")
    public Result<LedgerWalletDtos.WithBalancesResponse> createFull(
        @RequestParam String ownerId,
        @RequestParam String currency,
        @RequestParam(required = false) String extIdentifier,
        @RequestParam(required = false) String extType,
        @RequestParam(required = false) List<String> extraCurrencies
    ) {
        return R.success(createWalletUseCase.executeFull(ownerId, currency, extraCurrencies, extIdentifier, extType));
    }

    @PostMapping("/{id}/activations")
    public Result<LedgerWalletDtos.WithBalancesResponse> activate(
        @PathVariable Long id,
        @RequestBody(required = false) LedgerWalletDtos.ActivationRequest dto
    ) {
        return R.success(activateWalletUseCase.execute(id, dto == null
            ? new LedgerWalletDtos.ActivationRequest(null, null) : dto));
    }

    @PutMapping("/{id}/statuses")
    public Result<LedgerWalletDtos.WithBalancesResponse> updateStatuses(
        @PathVariable Long id,
        @Valid @RequestBody LedgerWalletDtos.UpdateRequest putDto
    ) {
        return R.success(updateWalletUseCase.execute(id, putDto));
    }

    @GetMapping("/{id}")
    public Result<LedgerWalletDtos.WithBalancesResponse> getOne(
        @PathVariable Long id,
        @RequestParam(required = false) String fxTarget
    ) {
        return R.success(queryWalletBalanceUseCase.one(id, fxTarget));
    }

    @GetMapping
    public Result<List<LedgerWalletDtos.WithBalancesResponse>> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) String fxTarget
    ) {
        var page = queryWalletBalanceUseCase.list(pageable, fxTarget);
        return R.success(page.getContent(), Pagination.create(page));
    }

    @GetMapping("/my-wallets")
    public Result<List<LedgerWalletDtos.WithBalancesResponse>> myWallets(
        @RequestParam String ownerId,
        @RequestParam(required = false) String fxTarget
    ) {
        if (fxTarget == null || fxTarget.isBlank()) {
            return R.success(queryMyWalletUseCase.execute(ownerId));
        }
        return R.success(queryWalletBalanceUseCase.myWallets(ownerId, fxTarget));
    }

    @GetMapping("/ext/{type}/{id}")
    public Result<LedgerWalletDtos.WithBalancesResponse> getByExtIdentifier(
        @PathVariable String type,
        @PathVariable String id
    ) {
        return R.success(queryWalletBalanceUseCase.byExtIdentifier(id, type));
    }
}
