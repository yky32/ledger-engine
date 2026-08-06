package com.altech.ledger.endpoint.usecase.business_operation.recipients;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.usecase.setup.LinkedBankAccountUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/linked-bank-accounts")
public class LinkedBankAccountEndpoint {
    private final LinkedBankAccountUseCase useCase;

    public LinkedBankAccountEndpoint(LinkedBankAccountUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkedBankAccountResponse create(@Valid @RequestBody CreateLinkedBankAccountRequest dto) {
        return useCase.create(dto);
    }

    @GetMapping("/{id}")
    public LinkedBankAccountResponse getById(@PathVariable Long id) {
        return useCase.getById(id);
    }

    @PutMapping("/{id}")
    public LinkedBankAccountResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLinkedBankAccountRequest dto
    ) {
        return useCase.update(id, dto);
    }

    @GetMapping("/my-accounts")
    public List<LinkedBankAccountResponse> myAccounts(@RequestParam Long tenantId) {
        return useCase.getMyAccount(tenantId);
    }
}
