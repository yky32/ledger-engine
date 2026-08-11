package com.altech.ledger.repository;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.po.ledger.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByAlias(String alias);

    /** 1 CUST : 1 Wallet — primary lookup. */
    Optional<Wallet> findByOwnerId(String ownerId);

    boolean existsByOwnerId(String ownerId);

    /**
     * @deprecated currency is default settlement only; use {@link #findByOwnerId(String)}.
     * Kept for transitional call sites; ignores multi-wallet-per-currency model.
     */
    @Deprecated
    default Optional<Wallet> findByOwnerIdAndCurrency(String ownerId, Currency currency) {
        return findByOwnerId(ownerId);
    }

    /**
     * @deprecated use {@link #existsByOwnerId(String)}.
     */
    @Deprecated
    default boolean existsByOwnerIdAndCurrency(String ownerId, Currency currency) {
        return existsByOwnerId(ownerId);
    }

    /** @deprecated 1:1 model returns 0..1; prefer {@link #findByOwnerId(String)}. */
    @Deprecated
    default List<Wallet> findAllByOwnerId(String ownerId) {
        return findByOwnerId(ownerId).map(List::of).orElseGet(List::of);
    }

    Optional<Wallet> findByAssociatedIdentifierAndAssociatedFrom(String associatedIdentifier, String associatedFrom);

    List<Wallet> findByAssociatedIdentifier(String associatedIdentifier);

    Optional<Wallet> findByAccountId(Long accountId);

    boolean existsByAlias(String alias);
}
