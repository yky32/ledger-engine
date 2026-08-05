package com.altech.ledger.repository;

import com.altech.ledger.entity.po.LedgerMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerMovementRepository extends JpaRepository<LedgerMovement, UUID> {
    Optional<LedgerMovement> findByMovementKey(String movementKey);
    Page<LedgerMovement> findByWalletId(UUID walletId, Pageable pageable);
}
