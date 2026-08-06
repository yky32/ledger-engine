package com.altech.ledger.endpoint.usecase.wallet_transfer;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
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
public class WalletTransferEndpoint {
    private final LedgerMovementPipelineUseCase pipeline;

    public WalletTransferEndpoint(LedgerMovementPipelineUseCase pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/ledger/wallet-transfers/in-wallet")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse inWallet(@Valid @RequestBody CreateInWalletTransferRequest dto) {
        return pipeline.inWalletTransfer(dto);
    }

    @PostMapping(value = "/ledger/wallet-transfers/in-wallet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse inWalletMultipart(
        @RequestParam String fromWalletId,
        @RequestParam String toWalletId,
        @RequestParam String currency,
        @RequestParam BigDecimal amount,
        @RequestParam(required = false) String movementKey,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) List<MultipartFile> files
    ) {
        String fileMeta = MultipartFileMetadata.summarize(files);
        CreateInWalletTransferRequest req = new CreateInWalletTransferRequest(
            fromWalletId, toWalletId, currency, amount, LedgerMovementMode.MANUAL, movementKey,
            description == null ? fileMeta : description + " files=" + fileMeta);
        MovementResponse created = pipeline.inWalletTransfer(req);
        if (fileMeta != null) {
            return pipeline.updateDocuments(created.id(), new UpdateTransferDocumentsRequest(fileMeta, null));
        }
        return created;
    }

    @PutMapping("/ledger/wallet-transfers/{id}/documents")
    public MovementResponse updateDocuments(
        @PathVariable Long id,
        @Valid @RequestBody UpdateTransferDocumentsRequest dto
    ) {
        return pipeline.updateDocuments(id, dto);
    }

    @PutMapping(value = "/ledger/wallet-transfers/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MovementResponse updateDocumentsMultipart(
        @PathVariable Long id,
        @RequestParam(required = false) String remarks,
        @RequestParam(required = false) List<MultipartFile> files
    ) {
        return pipeline.updateDocuments(id,
            new UpdateTransferDocumentsRequest(MultipartFileMetadata.summarize(files), remarks));
    }
}
