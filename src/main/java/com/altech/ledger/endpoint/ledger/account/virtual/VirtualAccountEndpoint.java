package com.altech.ledger.endpoint.ledger.account.virtual;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.usecase.account.virtual.VirtualAccountApplicationUseCase;
import com.altech.ledger.usecase.account.virtual.VirtualAccountUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/virtual-accounts")
public class VirtualAccountEndpoint {
    private final VirtualAccountApplicationUseCase applicationUseCase;
    private final VirtualAccountUseCase virtualAccountUseCase;

    public VirtualAccountEndpoint(VirtualAccountApplicationUseCase applicationUseCase,
                                  VirtualAccountUseCase virtualAccountUseCase) {
        this.applicationUseCase = applicationUseCase;
        this.virtualAccountUseCase = virtualAccountUseCase;
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public VirtualAccountApplicationResponse apply(
        @Valid @RequestBody CreateVirtualAccountApplicationRequest dto
    ) {
        return applicationUseCase.apply(dto);
    }

    @GetMapping("/applications/{id}")
    public VirtualAccountApplicationResponse getApplication(@PathVariable Long id) {
        return applicationUseCase.getApplication(id);
    }

    @PatchMapping("/applications/{id}/status")
    public VirtualAccountApplicationResponse patchStatus(
        @PathVariable Long id,
        @Valid @RequestBody PatchVirtualAccountApplicationStatusRequest dto
    ) {
        return applicationUseCase.patchStatus(id, dto);
    }

    @PatchMapping("/applications/{id}/metadata")
    public VirtualAccountApplicationResponse patchMetadata(
        @PathVariable Long id,
        @Valid @RequestBody PatchVirtualAccountApplicationMetadataRequest dto
    ) {
        return applicationUseCase.patchMetadata(id, dto);
    }

    @GetMapping("/applications")
    public Page<VirtualAccountApplicationResponse> listApplications(
        @PageableDefault(size = 50) Pageable pageable
    ) {
        return applicationUseCase.listApplications(pageable);
    }

    @GetMapping("/me")
    public List<VirtualAccountResponse> me(@RequestParam String extIdentifier) {
        return virtualAccountUseCase.me(extIdentifier);
    }

    @GetMapping
    public Page<VirtualAccountResponse> list(@PageableDefault(size = 50) Pageable pageable) {
        return virtualAccountUseCase.list(pageable);
    }
}
