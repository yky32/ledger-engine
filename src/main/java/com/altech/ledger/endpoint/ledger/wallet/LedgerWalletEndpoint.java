package com.altech.ledger.endpoint.ledger.wallet;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.usecase.account.QueryWalletBalanceUseCase;
import com.altech.ledger.usecase.wallet.ActivateWalletUseCase;
import com.altech.ledger.usecase.wallet.CreateWalletUseCase;
import com.altech.ledger.usecase.wallet.QueryMyWalletUseCase;
import com.altech.ledger.usecase.wallet.UpdateWalletUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.altech.ledger.entity.dto.request.ActivateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.request.CreateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.request.UpdateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;

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
    public Result<GetLedgerWalletResponseDto> create(@Valid @RequestBody CreateLedgerWalletRequestDto dto) {
        return R.success(createWalletUseCase.execute(dto));
    }

    @PostMapping("/full")
    public Result<GetLedgerWalletResponseDto> createFull(
        @RequestParam String ownerId,
        @RequestParam String currency,
        @RequestParam(required = false) String associatedIdentifier,
        @RequestParam(required = false) String associatedFrom,
        @RequestParam(required = false) List<String> extraCurrencies
    ) {
        return R.success(createWalletUseCase.executeFull(ownerId, currency, extraCurrencies, associatedIdentifier, associatedFrom));
    }

    @PostMapping("/{id}/activations")
    public Result<GetLedgerWalletResponseDto> activate(
        @PathVariable Long id,
        @RequestBody(required = false) ActivateLedgerWalletRequestDto dto
    ) {
        return R.success(activateWalletUseCase.execute(id, dto == null
            ? new ActivateLedgerWalletRequestDto(null, null) : dto));
    }

    @PutMapping("/{id}/statuses")
    public Result<GetLedgerWalletResponseDto> updateStatuses(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLedgerWalletRequestDto putDto
    ) {
        return R.success(updateWalletUseCase.execute(id, putDto));
    }

    @GetMapping("/{id}")
    public Result<GetLedgerWalletResponseDto> getOne(
        @PathVariable Long id,
        @RequestParam(required = false) String fxTarget
    ) {
        return R.success(queryWalletBalanceUseCase.one(id, fxTarget));
    }

    @GetMapping
    public Result<List<GetLedgerWalletResponseDto>> getAll(
        @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false) String fxTarget
    ) {
        var page = queryWalletBalanceUseCase.list(pageable, fxTarget);
        return R.success(page.getContent(), Pagination.create(page));
    }

    @GetMapping("/my-wallets")
    public Result<List<GetLedgerWalletResponseDto>> myWallets(
        @RequestParam String ownerId,
        @RequestParam(required = false) String fxTarget
    ) {
        if (fxTarget == null || fxTarget.isBlank()) {
            return R.success(queryMyWalletUseCase.execute(ownerId));
        }
        return R.success(queryWalletBalanceUseCase.myWallets(ownerId, fxTarget));
    }

    @GetMapping("/ext/{type}/{id}")
    public Result<GetLedgerWalletResponseDto> getByAssociatedIdentifier(
        @PathVariable String type,
        @PathVariable String id
    ) {
        return R.success(queryWalletBalanceUseCase.byAssociatedIdentifier(id, type));
    }
}
