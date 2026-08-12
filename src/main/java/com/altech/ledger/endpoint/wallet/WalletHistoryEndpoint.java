package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetAsOfBalanceResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.usecase.wallet.WalletHistoryQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Movement history filters + balance as-of (audit).
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletHistoryEndpoint {
    private final WalletHistoryQueryUseCase walletHistoryQueryUseCase;

    /**
     * GET /wallets/{associatedIdentifier}/movements?orderType=&currency=&status=&from=&to=&page=&size=
     */
    @GetMapping("/{associatedIdentifier}/movements")
    public Result<Map<String, Object>> movements(
        @PathVariable String associatedIdentifier,
        @RequestParam(required = false) String orderType,
        @RequestParam(required = false) String currency,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Page<GetLedgerMovementResponseDto> p = walletHistoryQueryUseCase.history(
            associatedIdentifier, orderType, currency, status, from, to, page, size);
        Map<String, Object> body = new HashMap<>();
        body.put("content", p.getContent());
        body.put("page", p.getNumber());
        body.put("size", p.getSize());
        body.put("totalElements", p.getTotalElements());
        body.put("totalPages", p.getTotalPages());
        return R.success(body);
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
