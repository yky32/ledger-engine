package com.altech.ledger.repository;

import com.altech.ledger.entity.enu.CoaDictionaryKind;
import com.altech.ledger.entity.po.coa.CoaDictionary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoaDictionaryRepository extends JpaRepository<CoaDictionary, Long> {
    Optional<CoaDictionary> findByKindAndCode(CoaDictionaryKind kind, String code);

    boolean existsByKindAndCode(CoaDictionaryKind kind, String code);

    List<CoaDictionary> findAllByIsActiveTrueOrderByKindAscCodeAsc();
}
