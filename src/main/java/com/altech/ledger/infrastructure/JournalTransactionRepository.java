package com.altech.ledger.infrastructure;

import com.altech.ledger.domain.JournalTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JournalTransactionRepository extends JpaRepository<JournalTransaction, UUID> {
    @EntityGraph(attributePaths = {"entries", "entries.account", "reversalOf"})
    @Query("select t from JournalTransaction t where t.id = :id")
    Optional<JournalTransaction> findWithEntriesById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"entries", "entries.account", "reversalOf"})
    Optional<JournalTransaction> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"entries", "entries.account"})
    Optional<JournalTransaction> findByReversalOfId(UUID originalId);
}
