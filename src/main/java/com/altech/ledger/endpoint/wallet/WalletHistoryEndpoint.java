package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.Pagination;
import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetAsOfBalanceResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.usecase.wallet.WalletHistoryQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Movement history filters + balance as-of (audit).
 * Pagination matches tgt.profile: 1-based page + {@code startDt}/{@code endDt} strings.
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletHistoryEndpoint {
    private final WalletHistoryQueryUseCase walletHistoryQueryUseCase;

    /**
     * GET /wallets/{associatedIdentifier}/movements?orderType=&currency=&status=&startDt=&endDt=&page=&size=
     */
    @GetMapping("/{associatedIdentifier}/movements")
    public Result<List<GetLedgerMovementResponseDto>> movements(
        @PathVariable String associatedIdentifier,
        @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(required = false) String orderType,
        @RequestParam(required = false) String currency,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String startDt,
        @RequestParam(required = false) String endDt
    ) {
        Page<GetLedgerMovementResponseDto> page = walletHistoryQueryUseCase.history(
            associatedIdentifier, pageable, orderType, currency, status, startDt, endDt);
        return R.success(page.getContent(), Pagination.create(page));
    }

    /**
     * GET /wallets/{associatedIdentifier}/balances/as-of?at=ISO-8601&currency=LP
     */
    @GetMapping("/{associatedIdentifier}/balances/as-of")
    public Result<GetAsOfBalanceResponseDto> asOf(
        @PathVariable String associatedIdentifier,
        @RequestParam(required = false, name = "at")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at,
        @RequestParam(required = false) String currency
    ) {
        return R.success(walletHistoryQueryUseCase.asOf(associatedIdentifier, at, currency));
    }
}
