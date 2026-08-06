package com.altech.ledger.repository;

import com.altech.ledger.entity.po.journal.JournalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JournalTransactionRepository extends JpaRepository<JournalTransaction, UUID> {
    Optional<JournalTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<JournalTransaction> findByReversalOfId(UUID reversalOfId);

    @Query("select t from JournalTransaction t left join fetch t.entries where t.id = :id")
    Optional<JournalTransaction> findWithEntriesById(@Param("id") UUID id);
}
