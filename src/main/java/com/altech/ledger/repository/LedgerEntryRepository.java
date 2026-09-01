package com.altech.ledger.repository;

import com.altech.ledger.entity.po.log.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTxnId(Long txnId);

    List<LedgerEntry> findByTxnIdIn(java.util.Collection<Long> txnIds);

    /** asOf must be non-null (use Instant.now() for live rebuild). */
    @Query("""
        select e from LedgerEntry e
        where e.targetId = :accountId
          and e.createDt <= :asOf
        order by e.createDt asc, e.id asc
        """)
    List<LedgerEntry> findForAsOf(
        @Param("accountId") String accountId,
        @Param("asOf") Instant asOf
    );
}
