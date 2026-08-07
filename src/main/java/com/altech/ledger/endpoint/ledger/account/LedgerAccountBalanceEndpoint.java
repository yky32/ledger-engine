package com.altech.ledger.endpoint.ledger.account;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.account.WalletAccountBalanceUseCase;
import com.altech.ledger.usecase.setup.AccountSetupUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.LedgerAccountDtos;

@RestController
@RequestMapping("/ledger-accounts")
@RequiredArgsConstructor
public class LedgerAccountBalanceEndpoint {
    private final AccountSetupUseCase accountSetupUseCase;
    private final WalletAccountBalanceUseCase balanceUseCase;

    @GetMapping("/{id}/balances")
    public Result<LedgerAccountDtos.Response> getBalance(@PathVariable Long id) {
        return R.success(accountSetupUseCase.getOne(id));
    }

    @GetMapping("/balances")
    public Result<List<LedgerAccountDtos.BalanceResponse>> getAllBalances() {
        return R.success(balanceUseCase.getAllBalances());
    }

    @GetMapping("/my-balances")
    public Result<List<LedgerAccountDtos.BalanceResponse>> myBalances(@RequestParam String ownerId) {
        return R.success(balanceUseCase.myBalances(ownerId));
    }
}
