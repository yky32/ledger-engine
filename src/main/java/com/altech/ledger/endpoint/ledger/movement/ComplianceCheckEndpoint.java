package com.altech.ledger.endpoint.ledger.movement;

import com.altech.ledger.entity.dto.parity.ParityDtos.MovementResponse;
import com.altech.ledger.entity.dto.parity.ParityDtos.UpdateComplianceRequest;
import com.altech.ledger.usecase.ledger.LedgerMovementOperationUseCase;
import com.altech.ledger.util.MultipartFileMetadata;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ledger-accounts/movements")
public class ComplianceCheckEndpoint {
    private final LedgerMovementOperationUseCase operationUseCase;

    public ComplianceCheckEndpoint(LedgerMovementOperationUseCase operationUseCase) {
        this.operationUseCase = operationUseCase;
    }

    @PutMapping("/{id}/compliance")
    public MovementResponse updateCompliance(
        @PathVariable Long id,
        @Valid @RequestBody UpdateComplianceRequest dto
    ) {
        return operationUseCase.updateCompliance(id, dto);
    }

    @PutMapping(value = "/{id}/compliance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MovementResponse updateComplianceMultipart(
        @PathVariable Long id,
        @RequestParam(required = false) String complianceContext,
        @RequestParam(required = false) String remarks,
        @RequestParam(required = false) List<MultipartFile> files
    ) {
        return operationUseCase.updateCompliance(id, new UpdateComplianceRequest(
            complianceContext, MultipartFileMetadata.summarize(files), remarks));
    }
}
