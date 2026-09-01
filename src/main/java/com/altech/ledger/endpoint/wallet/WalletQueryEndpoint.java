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

import java.util.List;

/**
 * Wallet Query API — list-all, or lookup by {@code ownerId}.
 */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletQueryEndpoint {
    private final QueryWalletUseCase queryWalletUseCase;

    /**
     * GET /wallets/{ownerId}
     * GET /wallets/{ownerId}?currencies=HKD,LP
     */
    @GetMapping("/{ownerId}")
    public Result<GetWalletOnboardResponseDto> getByOwnerId(
        @PathVariable String ownerId,
        @RequestParam(required = false) String currencies
    ) {
        return R.success(queryWalletUseCase.byOwnerId(ownerId, currencies));
    }

    /**
     * GET /wallets?ownerId=01A12345678
     * GET /wallets?ownerId=01A12345678&amp;currencies=LP
     */
    @GetMapping(params = "ownerId")
    public Result<GetWalletOnboardResponseDto> getByOwnerIdParam(
        @RequestParam String ownerId,
        @RequestParam(required = false) String currencies
    ) {
        return R.success(queryWalletUseCase.byOwnerId(ownerId, currencies));
    }

    /**
     * GET /wallets — list active wallets (no nested accounts).
     */
    @GetMapping
    public Result<List<GetWalletOnboardResponseDto>> listAll() {
        return R.success(queryWalletUseCase.listAll());
    }
}
