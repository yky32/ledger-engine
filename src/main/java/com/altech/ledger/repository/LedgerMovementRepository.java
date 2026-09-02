package com.altech.ledger.repository;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.log.LedgerMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface LedgerMovementRepository extends JpaRepository<LedgerMovement, Long> {
    Optional<LedgerMovement> findByMovementKey(String movementKey);

    Optional<LedgerMovement> findFirstByAssociatedLedgerMovementId(Long associatedLedgerMovementId);

    Page<LedgerMovement> findByWalletId(Long walletId, Pageable pageable);

    Page<LedgerMovement> findByWalletIdIn(java.util.Collection<Long> walletIds, Pageable pageable);

    /**
     * History search. Callers must pass non-null {@code from}/{@code to} bounds
     * (use epoch / far-future when unrestricted) so Postgres can type Instant params.
     */
    @Query("""
        select m from LedgerMovement m
        where m.walletId = :walletId
          and m.createDt >= :from
          and m.createDt <= :to
          and (:hasOrderType = false or m.orderType = :orderType)
          and (:hasCurrency = false or m.currency = :currency)
          and (:hasStatus = false or m.status = :status)
        order by m.createDt desc, m.id desc
        """)
    Page<LedgerMovement> search(
        @Param("walletId") Long walletId,
        @Param("hasOrderType") boolean hasOrderType,
        @Param("orderType") OrderType orderType,
        @Param("hasCurrency") boolean hasCurrency,
        @Param("currency") Currency currency,
        @Param("hasStatus") boolean hasStatus,
        @Param("status") LedgerMovementStatus status,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );
}
