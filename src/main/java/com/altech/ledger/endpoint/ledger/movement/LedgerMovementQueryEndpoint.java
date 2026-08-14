package com.altech.ledger.endpoint.ledger.movement;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.usecase.ledger.LedgerMovementQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @deprecated Product: {@code /movements} and {@code /wallets/{ownerId}/movements}.
 * Still served for in-cluster compatibility; do not use for new LedgeRX product work.
 * @see docs/TECH_DEBT.md TD-API-001
 */
@Deprecated(since = "coa-profile", forRemoval = false)
@RestController
@RequestMapping("/ledger-accounts/movements")
@RequiredArgsConstructor
public class LedgerMovementQueryEndpoint {
    private final LedgerMovementQueryUseCase ledgerMovementQueryUseCase;

    @GetMapping("/{id}")
    public Result<GetLedgerMovementResponseDto> getOne(@PathVariable Long id) {
        return R.success(ledgerMovementQueryUseCase.one(id));
    }

    @GetMapping
    public Result<List<GetLedgerMovementResponseDto>> getAll(
        @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false) String startDt,
        @RequestParam(required = false) String endDt,
        @RequestParam(required = false) List<String> statuses
    ) {
        var page = ledgerMovementQueryUseCase.list(pageable, startDt, endDt, statuses);
        return R.success(page.getContent(), Pagination.create(page));
    }

    @GetMapping("/my-movements")
    public Result<List<GetLedgerMovementResponseDto>> myMovements(
        @RequestParam String ownerId,
        @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false) String startDt,
        @RequestParam(required = false) String endDt,
        @RequestParam(required = false) List<String> statuses
    ) {
        var page = ledgerMovementQueryUseCase.myMovements(ownerId, pageable, startDt, endDt, statuses);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
