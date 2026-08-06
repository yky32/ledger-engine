package com.altech.ledger.endpoint.ledger.wallet;

import com.altech.ledger.entity.po.ledger.WalletApplication;
import com.altech.ledger.usecase.setup.WalletApplicationUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ledger-wallet-applications")
public class WalletApplicationEndpoint {
    private final WalletApplicationUseCase useCase;

    public WalletApplicationEndpoint(WalletApplicationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletApplication create(@RequestBody Map<String, String> body) {
        return useCase.create(
            body.get("extIdentifier"),
            body.get("extType"),
            body.get("requestBody"),
            body.get("alias"));
    }

    @GetMapping("/{id}")
    public WalletApplication get(@PathVariable Long id) {
        return useCase.get(id);
    }

    @GetMapping
    public Page<WalletApplication> list(@PageableDefault(size = 50) Pageable pageable) {
        return useCase.list(pageable);
    }

    @PostMapping("/{id}/complete")
    public WalletApplication complete(
        @PathVariable Long id,
        @RequestParam(required = false, defaultValue = "USD") String currency
    ) {
        return useCase.complete(id, currency);
    }

    @PostMapping("/{id}/fail")
    public WalletApplication fail(@PathVariable Long id) {
        return useCase.fail(id);
    }
}
