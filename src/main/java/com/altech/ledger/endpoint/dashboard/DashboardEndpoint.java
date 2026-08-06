package com.altech.ledger.endpoint.dashboard;

import com.altech.ledger.entity.dto.parity.ParityDtos.DashboardResponse;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboards")
public class DashboardEndpoint {
    private final WalletRepository wallets;
    private final AccountRepository accounts;
    private final LedgerMovementRepository movements;

    public DashboardEndpoint(WalletRepository wallets, AccountRepository accounts,
                             LedgerMovementRepository movements) {
        this.wallets = wallets;
        this.accounts = accounts;
        this.movements = movements;
    }

    @GetMapping
    public DashboardResponse summary() {
        long open = movements.findAll().stream()
            .filter(m -> m.getStatus() != LedgerMovementStatus.SETTLED
                && m.getStatus() != LedgerMovementStatus.REJECTED
                && m.getStatus() != LedgerMovementStatus.ERROR)
            .count();
        return new DashboardResponse(
            wallets.count(), accounts.count(), movements.count(), open);
    }
}
