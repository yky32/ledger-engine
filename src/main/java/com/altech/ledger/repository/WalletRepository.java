package com.altech.ledger.repository;

import com.altech.ledger.entity.po.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByAlias(String alias);
    Optional<Wallet> findByOwnerIdAndCurrency(String ownerId, String currency);
    List<Wallet> findByOwnerId(String ownerId);
    boolean existsByAlias(String alias);
    boolean existsByOwnerIdAndCurrency(String ownerId, String currency);
}
