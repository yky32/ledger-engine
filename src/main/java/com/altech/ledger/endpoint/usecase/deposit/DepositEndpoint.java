package com.altech.ledger.endpoint.usecase.deposit;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.usecase.ledger.LedgerDepositUseCase;
import com.altech.ledger.util.MultipartFileMetadata;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.movement.LedgerMovementDtos;

@RestController
@RequiredArgsConstructor
public class DepositEndpoint {
    private final LedgerDepositUseCase depositUseCase;

    @PostMapping("/ledger/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<LedgerMovementDtos.Response> deposit(@Valid @RequestBody LedgerMovementDtos.CreateDepositRequest dto) {
        return R.success(depositUseCase.execute(dto));
    }

    @PostMapping(value = "/ledger/deposits", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Result<LedgerMovementDtos.Response> depositMultipart(
        @RequestParam String targetWalletId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount,
        @RequestParam(required = false) String movementKey,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) List<MultipartFile> files
    ) {
        String fileMeta = MultipartFileMetadata.summarize(files);
        return R.success(depositUseCase.execute(new LedgerMovementDtos.CreateDepositRequest(
            targetWalletId, currency, amount, LedgerMovementMode.MANUAL, null, movementKey,
            description, fileMeta == null ? null : Map.of("files", fileMeta))));
    }

    @PostMapping("/ledger/deposits/card-session")
    public Result<Map<String, String>> cardSession(
        @RequestParam Long walletId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount
    ) {
        return R.success(depositUseCase.initiateCardDeposit(walletId, currency, amount, Map.of()));
    }
}
