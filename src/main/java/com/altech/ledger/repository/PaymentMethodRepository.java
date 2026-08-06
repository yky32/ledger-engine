package com.altech.ledger.repository;

import com.altech.ledger.entity.po.ledger.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByWalletId(Long walletId);
}
