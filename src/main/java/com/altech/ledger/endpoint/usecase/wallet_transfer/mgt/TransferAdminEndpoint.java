package com.altech.ledger.endpoint.usecase.wallet_transfer.mgt;

import com.altech.ledger.entity.dto.parity.ParityDtos.CreateSwiftTransferRequest;
import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.entity.enu.LedgerMovementMode;
import com.altech.ledger.usecase.ledger.LedgerMovementPipelineUseCase;
import com.altech.ledger.util.MultipartFileMetadata;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin/ledger/wallet-transfers")
public class TransferAdminEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    public TransferAdminEndpoint(LedgerMovementPipelineUseCase pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/swift")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse adminSwift(@Valid @RequestBody CreateSwiftTransferRequest dto) {
        return pipeline.swiftTransfer(dto);
    }

    @PostMapping(value = "/swift", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse adminSwiftMultipart(
        @RequestParam String fromWalletId,
        @RequestParam String targetId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount,
        @RequestParam(required = false) String movementKey,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) List<MultipartFile> files
    ) {
        return pipeline.swiftTransfer(new CreateSwiftTransferRequest(
            fromWalletId, targetId, currency, amount, LedgerMovementMode.MANUAL, movementKey,
            description, MultipartFileMetadata.summarize(files)));
    }
}
