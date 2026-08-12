package com.altech.ledger.usecase.wallet;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.entity.dto.response.GetAsOfBalanceResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerEntry;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WalletHistoryQueryUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> history(
        String associatedIdentifier,
        String orderType,
        String currency,
        String status,
        Instant from,
        Instant to,
        int page,
        int size
    ) {
        Wallet w = _wallet(associatedIdentifier);
        OrderType ot = null;
        if (orderType != null && !orderType.isBlank()) {
            ot = OrderType.valueOf(orderType.trim().toUpperCase(Locale.ROOT));
        }
        Currency ccy = null;
        if (currency != null && !currency.isBlank()) {
            ccy = Currency.get(currency.trim().toUpperCase(Locale.ROOT));
        }
        LedgerMovementStatus st = null;
        if (status != null && !status.isBlank()) {
            st = LedgerMovementStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        }
        int p = Math.max(0, page);
        int s = size <= 0 ? 20 : Math.min(size, 100);
        Instant fromBound = from == null ? Instant.EPOCH : from;
        Instant toBound = to == null ? Instant.parse("9999-12-31T23:59:59Z") : to;
        boolean hasOt = ot != null;
        boolean hasCcy = ccy != null;
        boolean hasSt = st != null;
        return ledgerMovementRepository.search(
            w.getId(),
            hasOt, ot,
            hasCcy, ccy,
            hasSt, st,
            fromBound, toBound,
            PageRequest.of(p, s)
        ).map(DtoMapper::toMovement);
    }

    @Transactional(readOnly = true)
    public GetAsOfBalanceResponseDto asOf(String associatedIdentifier, Instant asOf, String currencyFilter) {
        Wallet w = _wallet(associatedIdentifier);
        Instant point = asOf == null ? Instant.now() : asOf;
        Account primary = accountRepository.findById(w.getAccountId())
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "primary account missing"));
        List<Account> books = accountRepository.findAllByMainAccount(primary.getMainAccount());
        Currency filter = null;
        if (currencyFilter != null && !currencyFilter.isBlank()) {
            filter = Currency.get(currencyFilter.trim().toUpperCase(Locale.ROOT));
        }
        List<GetAsOfBalanceResponseDto.AccountAsOf> rows = new ArrayList<>();
        for (Account a : books) {
            if (filter != null && a.getCurrency() != filter) {
                continue;
            }
            BigDecimal ledger = BigDecimal.ZERO;
            BigDecimal available = BigDecimal.ZERO;
            for (LedgerEntry e : ledgerEntryRepository.findForAsOf(String.valueOf(a.getId()), point)) {
                BigDecimal signed = e.getDirection() == MovementDirection.CREDIT
                    ? e.getAmount() : e.getAmount().negate();
                if (e.isAffectsLedger()) {
                    ledger = ledger.add(signed);
                }
                if (e.isAffectsAvailable()) {
                    available = available.add(signed);
                }
            }
            rows.add(GetAsOfBalanceResponseDto.AccountAsOf.builder()
                .accountId(a.getId())
                .currency(a.getCurrency())
                .ledgerBalance(ledger)
                .availableBalance(available)
                .liveLedgerBalance(a.getLedgerBalance())
                .liveAvailableBalance(a.getAvailableBalance())
                .build());
        }
        return GetAsOfBalanceResponseDto.builder()
            .associatedIdentifier(associatedIdentifier)
            .walletId(w.getId())
            .asOf(point)
            .accounts(rows)
            .build();
    }

    private Wallet _wallet(String associatedIdentifier) {
        String id = associatedIdentifier == null ? "" : associatedIdentifier.trim();
        return walletRepository.findByOwnerId(id)
            .or(() -> walletRepository.findByAssociatedIdentifier(id).stream().findFirst())
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not found: " + id));
    }
}
