package com.altech.ledger.endpoint.usecase.wallet_transfer;

import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altech.ledger.entity.dto.movement.LedgerMovementDtos;

@RestController
@RequiredArgsConstructor
public class WalletTransferEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    @PostMapping("/ledger/wallet-transfers/in-wallet")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerMovementDtos.Response inWallet(@Valid @RequestBody LedgerMovementDtos.CreateInWalletTransferRequest dto) {
        return pipeline.inWalletTransfer(dto);
    }
}
