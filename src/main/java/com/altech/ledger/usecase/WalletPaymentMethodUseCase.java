package com.altech.ledger.usecase;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.enu.PaymentMethodStatus;
import com.altech.ledger.entity.po.ledger.PaymentMethod;
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.repository.PaymentMethodRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletPaymentMethodUseCase {
    private final WalletRepository wallets;
    private final PaymentMethodRepository paymentMethods;

    public WalletPaymentMethodUseCase(WalletRepository wallets, PaymentMethodRepository paymentMethods) {
        this.wallets = wallets;
        this.paymentMethods = paymentMethods;
    }

    @Transactional
    public PaymentMethodResponse create(Long walletId, CreatePaymentMethodRequest dto) {
        if (!wallets.existsById(walletId)) {
            throw LedgerException.notFound("Wallet not found: " + walletId);
        }
        PaymentMethodStatus status = dto.status() == null ? PaymentMethodStatus.ACTIVE : dto.status();
        PaymentMethod pm = new PaymentMethod(walletId, dto.type(), status, dto.metadata());
        pm.setTokenizationValue(dto.tokenizationValue());
        pm.setTokenizationProvider(dto.tokenizationProvider());
        return DtoMapper.toPaymentMethod(paymentMethods.save(pm));
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodResponse> get(Long walletId) {
        return paymentMethods.findByWalletId(walletId).stream().map(DtoMapper::toPaymentMethod).toList();
    }
}
