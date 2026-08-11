package com.altech.ledger.endpoint.wallet;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.usecase.wallet.QueryWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wallet <b>Query (R)</b> API.
 * <p>
 * Client key = {@code associatedIdentifier} (CUST_ID from create).<br>
 * Response = <b>Wallet → accounts[]</b>; optional {@code currencies} filters account rows.
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletQueryEndpoint {
    private final QueryWalletUseCase queryWalletUseCase;

    /**
     * Query wallet + accounts by CUST_ID.
     * <pre>
     * GET /wallets/01A12345678
     * GET /wallets/01A12345678?currencies=HKD,LP
     * </pre>
     *
     * @param currencies optional CSV filter, e.g. {@code HKD,LP} — only matching rows in {@code accounts[]}
     */
    @GetMapping("/{associatedIdentifier}")
    public Result<GetWalletOnboardResponseDto> getByAssociatedIdentifier(
        @PathVariable String associatedIdentifier,
        @RequestParam(required = false) String currencies
    ) {
        return R.success(queryWalletUseCase.byAssociatedIdentifier(associatedIdentifier, currencies));
    }

    /**
     * Same lookup via query params.
     * <pre>
     * GET /wallets?associatedIdentifier=01A12345678
     * GET /wallets?associatedIdentifier=01A12345678&amp;currencies=LP
     * </pre>
     */
    @GetMapping(params = "associatedIdentifier")
    public Result<GetWalletOnboardResponseDto> getByAssociatedIdentifierParam(
        @RequestParam String associatedIdentifier,
        @RequestParam(required = false) String currencies
    ) {
        return R.success(queryWalletUseCase.byAssociatedIdentifier(associatedIdentifier, currencies));
    }
}
