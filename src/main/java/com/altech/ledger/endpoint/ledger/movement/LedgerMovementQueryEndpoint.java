package com.altech.ledger.endpoint.ledger.movement;

import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.usecase.ledger.LedgerMovementQueryUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ledger-accounts/movements")
@RequiredArgsConstructor
public class LedgerMovementQueryEndpoint {
    private final LedgerMovementQueryUseCase queryUseCase;

    @GetMapping("/{id}")
    public MovementResponse getOne(@PathVariable Long id) {
        return queryUseCase.getOne(id);
    }

    @GetMapping
    public Page<MovementResponse> getAll(
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDt,
        @RequestParam(required = false) List<String> statuses
    ) {
        return queryUseCase.getAll(pageable, startDt, endDt, statuses);
    }

    @GetMapping("/my-movements")
    public Page<MovementResponse> myMovements(
        @RequestParam String ownerId,
        @PageableDefault(size = 50) Pageable pageable,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDt,
        @RequestParam(required = false) List<String> statuses
    ) {
        return queryUseCase.myMovements(ownerId, pageable, startDt, endDt, statuses);
    }
}
