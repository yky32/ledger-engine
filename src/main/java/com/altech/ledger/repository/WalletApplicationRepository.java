package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.WalletApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletApplicationRepository extends JpaRepository<WalletApplication, Long> {
    Optional<WalletApplication> findByReferenceHash(String referenceHash);
}
