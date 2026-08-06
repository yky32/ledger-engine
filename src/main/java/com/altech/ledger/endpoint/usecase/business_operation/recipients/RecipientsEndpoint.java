package com.altech.ledger.endpoint.usecase.business_operation.recipients;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.usecase.setup.RecipientSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recipients")
public class RecipientsEndpoint {
    private final RecipientSetupUseCase useCase;

    public RecipientsEndpoint(RecipientSetupUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{id}")
    public RecipientResponse getOne(@PathVariable Long id) {
        return useCase.getOne(id);
    }

    @GetMapping("/my-recipients")
    public List<RecipientResponse> myRecipients(@RequestParam Long tenantId) {
        return useCase.myRecipients(tenantId);
    }

    @GetMapping
    public Page<RecipientResponse> getAll(@PageableDefault(size = 50) Pageable pageable) {
        return useCase.getAll(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipientResponse create(@Valid @RequestBody CreateRecipientRequest dto) {
        return useCase.create(dto);
    }

    @PostMapping("/my-recipients")
    @ResponseStatus(HttpStatus.CREATED)
    public RecipientResponse createMy(@Valid @RequestBody CreateRecipientRequest dto) {
        return useCase.create(dto);
    }

    @PutMapping("/{id}/statuses")
    public RecipientResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateRecipientRequest dto
    ) {
        return useCase.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        useCase.delete(id);
    }
}
