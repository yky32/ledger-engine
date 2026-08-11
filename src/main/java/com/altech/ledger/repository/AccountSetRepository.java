package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.AccountSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountSetRepository extends JpaRepository<AccountSet, Long> {
    List<AccountSet> findByWalletIdOrderByIdAsc(Long walletId);

    Optional<AccountSet> findByWalletIdAndCode(Long walletId, String code);

    boolean existsByWalletIdAndCode(Long walletId, String code);
}
