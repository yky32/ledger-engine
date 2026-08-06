package com.altech.ledger.repository;

import com.altech.ledger.entity.po.journal.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    Page<JournalEntry> findByAccountId(Long accountId, Pageable pageable);

    @Query("""
        select coalesce(sum(case when e.side = com.altech.ledger.entity.po.journal.JournalEntry.Side.DEBIT
                                 then e.amount else 0 end), 0)
        from JournalEntry e where e.account.id = :accountId
        """)
    BigDecimal debitTotal(@Param("accountId") Long accountId);

    @Query("""
        select coalesce(sum(case when e.side = com.altech.ledger.entity.po.journal.JournalEntry.Side.CREDIT
                                 then e.amount else 0 end), 0)
        from JournalEntry e where e.account.id = :accountId
        """)
    BigDecimal creditTotal(@Param("accountId") Long accountId);
}
