package com.altech.ledger.repository;

import com.altech.ledger.entity.po.integration.IngestPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IngestPolicyRepository extends JpaRepository<IngestPolicy, Long> {

    @Query("select p from IngestPolicy p where p.isActive = true order by p.id asc")
    List<IngestPolicy> findActiveOrdered();

    default Optional<IngestPolicy> findFirstActive() {
        return findActiveOrdered().stream().findFirst();
    }
}
