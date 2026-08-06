package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByAlias(String alias);

    Optional<Wallet> findByOwnerIdAndCurrency(String ownerId, String currency);

    List<Wallet> findByOwnerId(String ownerId);

    Optional<Wallet> findByExtIdentifierAndExtType(String extIdentifier, String extType);

    List<Wallet> findByExtIdentifier(String extIdentifier);

    Optional<Wallet> findByAccountId(Long accountId);

    boolean existsByAlias(String alias);

    boolean existsByOwnerIdAndCurrency(String ownerId, String currency);
}
