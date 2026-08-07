package com.altech.ledger.entity.dto.parity;

import com.altech.ledger.entity.enu.WalletAssociationType;
import com.altech.ledger.entity.enu.WalletStatus;
import com.altech.ledger.entity.enu.WalletType;

import java.time.Instant;
import java.util.List;

/**
 * Wallet setup, activation, and balance-bearing wallet views (parity {@code /ledger-wallets}).
 */
public final class LedgerWalletDtos {
    private LedgerWalletDtos() {}

    /**
     * Attach a wallet row to an existing account (owner, currency, external ids).
     */
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

    /**
     * Partial update of wallet status / account / external identity / nickname.
     */
    public record UpdateRequest(
        WalletStatus status,
        Long accountId,
        String extIdentifier,
        WalletAssociationType type,
        String nickname
    ) {}

    /**
     * Activation payload (optional workflow / account refs for parity clients).
     */
    public record ActivationRequest(
        String accountId,
        String workflowExecutionId
    ) {}

    /**
     * Wallet plus related multi-currency account balances under the main account.
     */
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
