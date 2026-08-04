package com.altech.ledger.infrastructure;

import com.altech.ledger.domain.LedgerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    boolean existsByExternalReference(String externalReference);

    java.util.Optional<LedgerAccount> findByExternalReference(String externalReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LedgerAccount a where a.id in :ids order by a.id")
    List<LedgerAccount> lockAllById(@Param("ids") List<UUID> ids);
}
