package com.altech.ledger.endpoint.usecase.wallet_transfer;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.altech.ledger.entity.dto.request.CreateLedgerInWalletTransferRequestDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;

@RestController
@RequiredArgsConstructor
public class WalletTransferEndpoint {
    private final LedgerMovementPipelineUseCase ledgerMovementPipelineUseCase;

    @PostMapping("/ledger/wallet-transfers/in-wallet")
    public Result<GetLedgerMovementResponseDto> inWallet(@Valid @RequestBody CreateLedgerInWalletTransferRequestDto dto) {
        return R.success(ledgerMovementPipelineUseCase.inWalletTransfer(dto));
    }
}
