package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.usecase.account.QueryWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QueryMyWalletUseCase {
    private final QueryWalletBalanceUseCase queryWalletBalanceUseCase;

    @Transactional(readOnly = true)
    public List<LedgerWalletDtos.WithBalancesResponse> execute(String ownerId) {
        return queryWalletBalanceUseCase.myWallets(ownerId, null);
    }
}
