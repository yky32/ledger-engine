package com.altech.ledger.endpoint.usecase.wallet_transfer;

import com.altech.ledger.entity.dto.parity.ParityDtos.CreateInWalletTransferRequest;
import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WalletTransferEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    @PostMapping("/ledger/wallet-transfers/in-wallet")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse inWallet(@Valid @RequestBody CreateInWalletTransferRequest dto) {
        return pipeline.inWalletTransfer(dto);
    }
}
