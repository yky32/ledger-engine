package com.altech.ledger.usecase.wallet;

import com.altech.ledger.entity.dto.request.UpdateLedgerWalletRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;
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
    public GetLedgerWalletResponseDto execute(Long id, UpdateLedgerWalletRequestDto dto) {
        Wallet wallet = commonUseCase.requireWallet(id);
        if (dto.status() != null) {
            wallet.setStatus(dto.status());
        }
        if (dto.accountId() != null) {
            wallet.setAccountId(dto.accountId());
        }
        if (dto.name() != null) {
            wallet.setName(dto.name().isBlank() ? null : dto.name().trim());
        }
        walletRepository.save(wallet);
        return queryWalletBalanceUseCase.one(wallet.getId(), null);
    }
}
