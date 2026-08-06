package com.altech.ledger.usecase;

import com.altech.ledger.entity.dto.parity.ParityDtos.WalletWithBalancesResponse;
import com.altech.ledger.usecase.account.WalletAccountBalanceUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Port of the-wallet-ledger MyWalletUseCase. */
@Service
public class MyWalletUseCase {
    private final WalletAccountBalanceUseCase balanceUseCase;

    public MyWalletUseCase(WalletAccountBalanceUseCase balanceUseCase) {
        this.balanceUseCase = balanceUseCase;
    }

    @Transactional(readOnly = true)
    public List<WalletWithBalancesResponse> execute(String ownerId) {
        return balanceUseCase.myWallets(ownerId, null);
    }
}
