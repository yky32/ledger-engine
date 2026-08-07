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
import com.altech.ledger.entity.dto.response.GetDashboardResponseDto;

@RestController
@RequestMapping("/dashboards")
@RequiredArgsConstructor
public class DashboardEndpoint {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerMovementRepository ledgerMovementRepository;

    @GetMapping
    public Result<GetDashboardResponseDto> summary() {
        long open = ledgerMovementRepository.findAll().stream()
            .filter(m -> m.getStatus() != LedgerMovementStatus.SETTLED
                && m.getStatus() != LedgerMovementStatus.REJECTED
                && m.getStatus() != LedgerMovementStatus.ERROR)
            .count();
        return R.success(new GetDashboardResponseDto(
            walletRepository.count(), accountRepository.count(), ledgerMovementRepository.count(), open));
    }
}
