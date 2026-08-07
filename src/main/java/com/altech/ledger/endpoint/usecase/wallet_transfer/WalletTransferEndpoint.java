package com.altech.ledger.endpoint.usecase.wallet_transfer;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;

@RestController
@RequiredArgsConstructor
public class WalletTransferEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    @PostMapping("/ledger/wallet-transfers/in-wallet")
    public Result<LedgerMovementDtos.Response> inWallet(@Valid @RequestBody LedgerMovementDtos.CreateInWalletTransferRequest dto) {
        return R.success(pipeline.inWalletTransfer(dto));
    }
}
