package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.VirtualSubAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VirtualSubAccountRepository extends JpaRepository<VirtualSubAccount, Long> {
    List<VirtualSubAccount> findByVirtualAccountId(Long virtualAccountId);
}
