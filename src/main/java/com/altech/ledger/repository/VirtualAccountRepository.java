package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.VirtualAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, Long> {
    Optional<VirtualAccount> findByExtIdentifier(String extIdentifier);
}
