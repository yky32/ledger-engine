package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ingest.FailedTransactionIngest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FailedTransactionIngestRepository extends JpaRepository<FailedTransactionIngest, Long> {
    List<FailedTransactionIngest> findByEventIdOrderByIdDesc(String eventId);

    List<FailedTransactionIngest> findByStatusOrderByIdDesc(String status);

    List<FailedTransactionIngest> findByOwnerIdOrderByIdDesc(String ownerId);

    @Query("""
        select f from FailedTransactionIngest f
        where (:status is null or f.status = :status)
          and (:ownerId is null or f.ownerId = :ownerId)
          and (:failureCode is null or f.failureCode = :failureCode)
        """)
    Page<FailedTransactionIngest> search(
        @Param("status") String status,
        @Param("ownerId") String ownerId,
        @Param("failureCode") String failureCode,
        Pageable pageable
    );

    Optional<FailedTransactionIngest> findById(Long id);
}
