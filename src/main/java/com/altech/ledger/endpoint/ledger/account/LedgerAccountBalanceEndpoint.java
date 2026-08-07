package com.altech.ledger.endpoint.ledger.account;

import com.altech.core.response.R;
import com.altech.core.response.Result;
import com.altech.ledger.entity.dto.parity.LedgerAccountDtos;
import com.altech.ledger.usecase.account.QueryAccountUseCase;
import com.altech.ledger.usecase.account.QueryWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ledger-accounts")
@RequiredArgsConstructor
public class LedgerAccountBalanceEndpoint {
    private final QueryAccountUseCase queryAccountUseCase;
    private final QueryWalletBalanceUseCase queryWalletBalanceUseCase;

    @GetMapping("/{id}/balances")
    public Result<LedgerAccountDtos.Response> getBalance(@PathVariable Long id) {
        return R.success(queryAccountUseCase.one(id));
    }

    @GetMapping("/balances")
    public Result<List<LedgerAccountDtos.BalanceResponse>> getAllBalances() {
        return R.success(queryWalletBalanceUseCase.allBalances());
    }

    @GetMapping("/my-balances")
    public Result<List<LedgerAccountDtos.BalanceResponse>> myBalances(@RequestParam String ownerId) {
        return R.success(queryWalletBalanceUseCase.myBalances(ownerId));
    }
}
