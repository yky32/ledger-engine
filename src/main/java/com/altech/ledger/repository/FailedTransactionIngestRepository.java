package com.altech.ledger.repository;

import com.altech.ledger.entity.po.integration.FailedTransactionIngest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedTransactionIngestRepository extends JpaRepository<FailedTransactionIngest, Long> {
    List<FailedTransactionIngest> findByEventIdOrderByIdDesc(String eventId);

    List<FailedTransactionIngest> findByStatusOrderByIdDesc(String status);
}
