package com.altech.ledger.repository;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.po.ledger.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByAlias(String alias);

    Optional<Wallet> findByOwnerIdAndCurrency(String ownerId, Currency currency);

    List<Wallet> findByOwnerId(String ownerId);

    Optional<Wallet> findByAssociatedIdentifierAndAssociatedFrom(String associatedIdentifier, String associatedFrom);

    List<Wallet> findByAssociatedIdentifier(String associatedIdentifier);

    Optional<Wallet> findByAccountId(Long accountId);

    boolean existsByAlias(String alias);

    boolean existsByOwnerIdAndCurrency(String ownerId, Currency currency);
}
