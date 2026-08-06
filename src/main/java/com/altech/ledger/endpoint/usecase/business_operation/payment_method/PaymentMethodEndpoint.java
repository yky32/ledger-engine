package com.altech.ledger.endpoint.usecase.business_operation.payment_method;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.usecase.WalletPaymentMethodUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PaymentMethodEndpoint {
    private final WalletPaymentMethodUseCase useCase;

    public PaymentMethodEndpoint(WalletPaymentMethodUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/ledger-wallets/{walletId}/payment-methods")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse create(
        @PathVariable Long walletId,
        @Valid @RequestBody CreatePaymentMethodRequest dto
    ) {
        return useCase.create(walletId, dto);
    }

    @GetMapping("/ledger-wallets/{walletId}/payment-methods")
    public List<PaymentMethodResponse> get(@PathVariable Long walletId) {
        return useCase.get(walletId);
    }
}
