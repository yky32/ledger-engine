package com.altech.ledger.endpoint.ledger.movement;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.usecase.ledger.LedgerMovementQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ledger-accounts/movements")
@RequiredArgsConstructor
public class LedgerMovementQueryEndpoint {
    private final LedgerMovementQueryUseCase ledgerMovementQueryUseCase;

    @GetMapping("/{id}")
    public Result<LedgerMovementDtos.Response> getOne(@PathVariable Long id) {
        return R.success(ledgerMovementQueryUseCase.one(id));
    }

    @GetMapping
    public Result<List<LedgerMovementDtos.Response>> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDt,
        @RequestParam(required = false) List<String> statuses
    ) {
        var page = ledgerMovementQueryUseCase.list(pageable, startDt, endDt, statuses);
        return R.success(page.getContent(), Pagination.create(page));
    }

    @GetMapping("/my-movements")
    public Result<List<LedgerMovementDtos.Response>> myMovements(
        @RequestParam String ownerId,
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDt,
        @RequestParam(required = false) List<String> statuses
    ) {
        var page = ledgerMovementQueryUseCase.myMovements(ownerId, pageable, startDt, endDt, statuses);
        return R.success(page.getContent(), Pagination.create(page));
    }
}
