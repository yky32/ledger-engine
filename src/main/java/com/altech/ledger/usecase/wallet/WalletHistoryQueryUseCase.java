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
import com.altech.ledger.exception.response.WalletErrorResponse;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.DtoWrapper;
import com.altech.ledger.util.Pageables;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WalletHistoryQueryUseCase {
    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public Page<GetLedgerMovementResponseDto> history(
        String ownerId,
        Pageable pageable,
        String orderType,
        String currency,
        String status,
        String startDt,
        String endDt
    ) {
        Wallet w = _wallet(ownerId);
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
        Instant fromBound = Pageables.parseStartDt(startDt);
        Instant toBound = Pageables.parseEndDt(endDt);
        Set<String> memberAccountIds = new HashSet<>();
        for (Account a : accountRepository.findAllByWalletId(w.getId())) {
            if (a.getId() != null) {
                memberAccountIds.add(String.valueOf(a.getId()));
            }
        }
        Page<GetLedgerMovementResponseDto> page = ledgerMovementRepository.search(
            w.getId(),
            ot != null, ot,
            ccy != null, ccy,
            st != null, st,
            fromBound, toBound,
            Pageables.toZeroBased(pageable)
        ).map(DtoWrapper::getLedgerMovementResponseDto);
        List<Long> ids = page.getContent().stream()
            .map(GetLedgerMovementResponseDto::id)
            .filter(id -> id != null)
            .toList();
        if (ids.isEmpty() || memberAccountIds.isEmpty()) {
            return page;
        }
        Map<Long, OrderType> orderById = new HashMap<>();
        for (GetLedgerMovementResponseDto d : page.getContent()) {
            if (d.id() != null) {
                orderById.put(d.id(), d.orderType());
            }
        }
        Map<Long, Currency> booked = new HashMap<>();
        for (LedgerEntry e : ledgerEntryRepository.findByTxnIdIn(ids)) {
            if (e.getTxnId() == null || e.getCurrency() == null || e.getTargetId() == null) {
                continue;
            }
            if (!memberAccountIds.contains(e.getTargetId())) {
                continue;
            }
            OrderType movementOt = orderById.get(e.getTxnId());
            boolean prefer = (movementOt == OrderType.EARN && e.getDirection() == MovementDirection.CREDIT)
                || (movementOt != OrderType.EARN && e.getDirection() == MovementDirection.DEBIT);
            Currency prev = booked.get(e.getTxnId());
            if (prev == null || prefer) {
                booked.put(e.getTxnId(), e.getCurrency());
            }
        }
        if (booked.isEmpty()) {
            return page;
        }
        return page.map(dto -> {
            Currency overlay = dto.id() == null ? null : booked.get(dto.id());
            return overlay == null || overlay == dto.currency()
                ? dto
                : DtoWrapper.getLedgerMovementResponseDto(dto, overlay);
        });
    }

    @Transactional(readOnly = true)
    public GetAsOfBalanceResponseDto asOf(String ownerId, Instant asOf, String currencyFilter) {
        Wallet w = _wallet(ownerId);
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
                .build());
        }
        return GetAsOfBalanceResponseDto.builder()
            .ownerId(ownerId)
            .walletId(w.getId())
            .asOf(point)
            .accounts(rows)
            .build();
    }

    private Wallet _wallet(String ownerId) {
        String id = ownerId == null ? "" : ownerId.trim();
        return walletRepository.findByOwnerId(id)
            .orElseThrow(() -> new BizException(WalletErrorResponse.WAL0404, "Wallet not found: " + id));
    }
}
