package com.altech.ledger.usecase.wallet;

import com.altech.ledger.usecase.account.QueryWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;

@Component
@RequiredArgsConstructor
public class QueryMyWalletUseCase {
    private final QueryWalletBalanceUseCase queryWalletBalanceUseCase;

    @Transactional(readOnly = true)
    public List<GetLedgerWalletResponseDto> execute(String ownerId) {
        return queryWalletBalanceUseCase.myWallets(ownerId, null);
    }
}
