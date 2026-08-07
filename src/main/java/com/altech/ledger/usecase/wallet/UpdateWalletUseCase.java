package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.usecase.CommonUseCase;
import com.altech.ledger.usecase.account.QueryWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UpdateWalletUseCase {
    private final WalletRepository walletRepository;
    private final CommonUseCase commonUseCase;
    private final QueryWalletBalanceUseCase queryWalletBalanceUseCase;

    @Transactional
    public LedgerWalletDtos.WithBalancesResponse execute(Long id, LedgerWalletDtos.UpdateRequest dto) {
        Wallet wallet = commonUseCase.requireWallet(id);
        if (dto.status() != null) {
            wallet.setStatus(dto.status());
        }
        if (dto.accountId() != null) {
            wallet.setAccountId(dto.accountId());
        }
        if (dto.extIdentifier() != null) {
            wallet.setExtIdentifier(dto.extIdentifier());
        }
        if (dto.type() != null) {
            wallet.setType(dto.type());
        }
        if (dto.nickname() != null) {
            wallet.setNickname(dto.nickname());
        }
        walletRepository.save(wallet);
        return queryWalletBalanceUseCase.one(wallet.getId(), null);
    }
}
