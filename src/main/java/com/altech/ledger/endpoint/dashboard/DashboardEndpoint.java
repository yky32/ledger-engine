package com.altech.ledger.endpoint.dashboard;

import com.altech.core.response.R;
import com.altech.core.response.Result;

import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.SystemDtos;

@RestController
@RequestMapping("/dashboards")
@RequiredArgsConstructor
public class DashboardEndpoint {
    private final WalletRepository wallets;
    private final AccountRepository accounts;
    private final LedgerMovementRepository movements;

    @GetMapping
    public Result<SystemDtos.DashboardResponse> summary() {
        long open = movements.findAll().stream()
            .filter(m -> m.getStatus() != LedgerMovementStatus.SETTLED
                && m.getStatus() != LedgerMovementStatus.REJECTED
                && m.getStatus() != LedgerMovementStatus.ERROR)
            .count();
        return R.success(new SystemDtos.DashboardResponse(
            wallets.count(), accounts.count(), movements.count(), open));
    }
}
