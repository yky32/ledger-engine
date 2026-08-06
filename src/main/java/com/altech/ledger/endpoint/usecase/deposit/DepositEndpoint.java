package com.altech.ledger.endpoint.usecase.deposit;

import com.altech.ledger.entity.dto.parity.ParityDtos.CreateDepositRequest;
import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
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

@RestController
public class DepositEndpoint {
    private final LedgerDepositUseCase depositUseCase;

    public DepositEndpoint(LedgerDepositUseCase depositUseCase) {
        this.depositUseCase = depositUseCase;
    }

    @PostMapping("/ledger/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse deposit(@Valid @RequestBody CreateDepositRequest dto) {
        return depositUseCase.execute(dto);
    }

    @PostMapping(value = "/ledger/deposits", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse depositMultipart(
        @RequestParam String targetWalletId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount,
        @RequestParam(required = false) String movementKey,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) List<MultipartFile> files
    ) {
        String fileMeta = MultipartFileMetadata.summarize(files);
        return depositUseCase.execute(new CreateDepositRequest(
            targetWalletId, currency, amount, LedgerMovementMode.MANUAL, null, movementKey,
            description, fileMeta == null ? null : Map.of("files", fileMeta)));
    }

    @PostMapping("/ledger/deposits/card-session")
    public Map<String, String> cardSession(
        @RequestParam Long walletId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount
    ) {
        return depositUseCase.initiateCardDeposit(walletId, currency, amount, Map.of());
    }
}
