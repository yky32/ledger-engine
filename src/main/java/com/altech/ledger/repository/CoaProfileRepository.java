package com.altech.ledger.repository;

import com.altech.ledger.entity.po.coa.CoaProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CoaProfileRepository extends JpaRepository<CoaProfile, Long> {
    Optional<CoaProfile> findByCode(String code);

    boolean existsByCode(String code);

    @Query("select p from CoaProfile p where p.isActive = true and p.isEnabled = true and p.isDefault = true order by p.id asc")
    List<CoaProfile> findEnabledDefaults();

    List<CoaProfile> findAllByIsActiveTrueOrderByCodeAsc();
}
