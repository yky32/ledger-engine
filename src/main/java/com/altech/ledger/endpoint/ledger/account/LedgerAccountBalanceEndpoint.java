package com.altech.ledger.endpoint.ledger.account;

import com.altech.ledger.entity.dto.parity.ParityDtos.AccountResponse;
import com.altech.ledger.entity.dto.parity.ParityDtos.BalanceResponse;
import com.altech.ledger.usecase.account.WalletAccountBalanceUseCase;
import com.altech.ledger.usecase.setup.AccountSetupUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ledger-accounts")
@RequiredArgsConstructor
public class LedgerAccountBalanceEndpoint {
    private final AccountSetupUseCase accountSetupUseCase;
    private final WalletAccountBalanceUseCase balanceUseCase;

    @GetMapping("/{id}/balances")
    public AccountResponse getBalance(@PathVariable Long id) {
        return accountSetupUseCase.getOne(id);
    }

    @GetMapping("/balances")
    public List<BalanceResponse> getAllBalances() {
        return balanceUseCase.getAllBalances();
    }

    @GetMapping("/my-balances")
    public List<BalanceResponse> myBalances(@RequestParam String ownerId) {
        return balanceUseCase.myBalances(ownerId);
    }
}
