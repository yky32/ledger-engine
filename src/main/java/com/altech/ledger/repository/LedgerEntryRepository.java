package com.altech.ledger.repository;

import com.altech.ledger.entity.po.log.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTxnId(Long txnId);
}
