package com.altech.ledger.repository;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.po.ledger.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /** 1 CUST : 1 Wallet — primary lookup. */
    Optional<Wallet> findByOwnerId(String ownerId);

    List<Wallet> findAllByIsActiveTrueOrderByCreateDtDesc();

    boolean existsByOwnerId(String ownerId);

    Optional<Wallet> findByAccountId(Long accountId);

    Optional<Wallet> findByVanityCode(String vanityCode);

    boolean existsByVanityCode(String vanityCode);

    /**
     * @deprecated currency is settlement only; use {@link #findByOwnerId(String)}.
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

    /** @deprecated 1:1 model; prefer {@link #findByOwnerId(String)}. */
    @Deprecated
    default List<Wallet> findAllByOwnerId(String ownerId) {
        return findByOwnerId(ownerId).map(List::of).orElseGet(List::of);
    }
}
