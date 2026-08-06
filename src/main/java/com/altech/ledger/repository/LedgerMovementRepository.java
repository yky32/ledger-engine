package com.altech.ledger.repository;

import com.altech.ledger.entity.po.log.LedgerMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerMovementRepository extends JpaRepository<LedgerMovement, Long> {
    Optional<LedgerMovement> findByMovementKey(String movementKey);

    Page<LedgerMovement> findByWalletId(Long walletId, Pageable pageable);

    Page<LedgerMovement> findByWalletIdIn(java.util.Collection<Long> walletIds, Pageable pageable);
}
