package com.altech.ledger.repository;

import com.altech.ledger.entity.po.wallet.WalletTierPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WalletTierPolicyRepository extends JpaRepository<WalletTierPolicy, Long> {

    @Query("select p from WalletTierPolicy p where p.isActive = true order by p.id asc")
    List<WalletTierPolicy> findActiveOrdered();

    default Optional<WalletTierPolicy> findFirstActive() {
        return findActiveOrdered().stream().findFirst();
    }
}
