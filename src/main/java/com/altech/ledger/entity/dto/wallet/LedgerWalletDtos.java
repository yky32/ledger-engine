package com.altech.ledger.entity.dto.wallet;

import com.altech.ledger.entity.dto.account.LedgerAccountDtos;
import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;

import java.time.Instant;
import java.util.List;

/** Wallet setup, activation, and balance-bearing wallet views. */
public final class LedgerWalletDtos {
    private LedgerWalletDtos() {}

    public record CreateRequest(
        Long accountId,
        String extIdentifier,
        String extType,
        WalletAssociationType type,
        String ownerId,
        String currency,
        String nickname
    ) {
        public CreateRequest {
            if (currency != null) {
                currency = currency.trim().toUpperCase();
            }
        }
    }

    public record UpdateRequest(
        WalletStatus status,
        Long accountId,
        String extIdentifier,
        WalletAssociationType type,
        String nickname
    ) {}

    public record ActivationRequest(
        String accountId,
        String workflowExecutionId
    ) {}

    public record WithBalancesResponse(
        Long id,
        String alias,
        Long accountId,
        String nickname,
        String extIdentifier,
        String extType,
        WalletAssociationType type,
        WalletType walletType,
        WalletStatus status,
        String ownerId,
        String currency,
        List<LedgerAccountDtos.Response> accounts,
        Instant createDt,
        Instant updateDt
    ) {}
}
