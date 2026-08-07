package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.account.QueryWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altech.ledger.entity.dto.request.ActivateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;

@Component
@RequiredArgsConstructor
public class ActivateWalletUseCase {
    private final WalletRepository walletRepository;
    private final CommonUseCase commonUseCase;
    private final QueryWalletBalanceUseCase queryWalletBalanceUseCase;

    @Transactional
    public GetLedgerWalletResponseDto execute(Long id) {
        Wallet wallet = commonUseCase.requireWallet(id);
        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
        return queryWalletBalanceUseCase.one(wallet.getId(), null);
    }

    @Transactional
    public GetLedgerWalletResponseDto execute(Long id, ActivateLedgerWalletRequestDto ignored) {
        return execute(id);
    }
}
