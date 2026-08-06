package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.VirtualAccountApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirtualAccountApplicationRepository extends JpaRepository<VirtualAccountApplication, Long> {
}
