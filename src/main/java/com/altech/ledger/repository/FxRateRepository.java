package com.altech.ledger.repository;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.po.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {
    Optional<FxRate> findByBaseAndTarget(Currency base, Currency target);
}
