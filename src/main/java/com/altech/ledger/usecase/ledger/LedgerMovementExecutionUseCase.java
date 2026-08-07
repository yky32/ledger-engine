package com.altech.ledger.usecase.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.core.exception.BizException;
import com.altech.ledger.exception.response.MovementErrorResponse;
import com.altech.ledger.exception.response.AccountErrorResponse;

import com.altech.ledger.entity.dto.BalanceExecutionResultCommand;
import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.BalanceOperation;
import com.altech.ledger.entity.enu.LedgerMovementStatus;
import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerEntry;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.listener.intf.LedgerHandler;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.MovementBus;
import com.altech.ledger.usecase.rule.QueryRuleExecutionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * LedgerMovementExecutionUseCase.
 * Structure: fetch rules → rulesExecution (command) → updateBalances → ledger ledgerEntryRepository.
 */
@Service
@RequiredArgsConstructor
public class LedgerMovementExecutionUseCase implements LedgerHandler {
    private static final Logger log = LoggerFactory.getLogger(LedgerMovementExecutionUseCase.class);

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final LedgerMovementRepository ledgerMovementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    /** @Lazy breaks cycle MovementBus ↔ this use case */
    @Lazy
    private final MovementBus movementBus;
    private final QueryRuleExecutionUseCase queryRuleExecutionUseCase;

    @Override
    @Transactional
    public void execute(LedgerMovementEvent event) {
        if (event.getMovementId() == null) {
            throw new BizException(MovementErrorResponse.MOV0400, "movementId required on event");
        }
        LedgerMovement movement = ledgerMovementRepository.findById(event.getMovementId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Movement not found: " + event.getMovementId()));
        execute(movement);
    }

    @Override
    public List<Account> fetchAccounts(String identifier) {
        try {
            Long id = Long.valueOf(identifier);
            return accountRepository.findById(id).map(List::of).orElse(List.of());
        } catch (NumberFormatException ex) {
            return List.of();
        }
    }

    @Override
    @Transactional
    public void updateBalances(LedgerMovementEvent event) {
        execute(event);
    }

    @Override
    public void notification(LedgerMovementEvent event) {
        log.info("movement notification id={} status={}", event.getMovementId(), event.getStatus());
    }

    @Transactional
    public LedgerMovement execute(LedgerMovement movement) {
        log.info("execute movement id={} orderType={} amount={}", movement.getId(),
            movement.getOrderType(), movement.getAmount());
        if (movement.getStatus() == LedgerMovementStatus.SETTLED) {
            return movement;
        }
        try {
            // Old always fetched accounting rules by OrderType; empty rules list still OK
            queryRuleExecutionUseCase.findByOrderType(movement.getOrderType()).ifPresent(re ->
                log.debug("RuleExecution found for {}: {}", movement.getOrderType(), re.name()));

            BalanceExecutionResultCommand command = rulesExecution(movement);
            applyCommand(command, movement);
            movement.setStatus(LedgerMovementStatus.SETTLED);
            ledgerMovementRepository.save(movement);
            createLedgerEntries(movement, command);
            movementBus.publishDone(movement);
            notification(MovementBus.toEvent(movement));
        } catch (RuntimeException ex) {
            log.error("movement execution failed id={}", movement.getId(), ex);
            movement.setStatus(LedgerMovementStatus.ERROR);
            ledgerMovementRepository.save(movement);
            throw ex;
        }
        return movement;
    }

    /**
     * rulesExecution — builds BalanceExecutionResultCommand from OrderType.
         */
    public BalanceExecutionResultCommand rulesExecution(LedgerMovement movement) {
        BalanceExecutionResultCommand command = new BalanceExecutionResultCommand();
        BigDecimal amount = movement.getAmount();
        Currency currency = movement.getCurrency();

        switch (movement.getOrderType()) {
            case DEPOSIT, PAYMENT_LINK -> {
                Account target = resolveAccount(movement.getTargetId(), currency);
                command.add(target, amount, BalanceOperation.ADD);
            }
            case WITHDRAWAL -> {
                Account origin = resolveAccount(movement.getOriginatorId(), currency);
                command.add(origin, amount, BalanceOperation.SUBTRACT);
            }
            case WALLET_TRANSFER, IN_WALLET_TRANSFER -> {
                Account origin = resolveAccount(movement.getOriginatorId(), currency);
                Account target = resolveAccount(movement.getTargetId(), currency);
                command.add(origin, amount, BalanceOperation.SUBTRACT);
                command.add(target, amount, BalanceOperation.ADD);
            }
            case SWIFT_TRANSFER -> {
                Account origin = resolveAccount(movement.getOriginatorId(), currency);
                command.add(origin, amount, BalanceOperation.SUBTRACT);
            }
            case EARN, ADJUSTMENT -> {
                Account target = resolveAccount(movement.getTargetId() != null
                    ? movement.getTargetId() : String.valueOf(movement.getWalletId()), currency);
                command.add(target, amount, BalanceOperation.ADD);
            }
            case BURN, BANK_CHARGE, HANDLING_CHARGE, CHARGE -> {
                Account origin = resolveAccount(movement.getOriginatorId() != null
                    ? movement.getOriginatorId() : String.valueOf(movement.getWalletId()), currency);
                command.add(origin, amount, BalanceOperation.SUBTRACT);
            }
            default -> throw new BizException(MovementErrorResponse.MOV0400, "Unsupported order type: " + movement.getOrderType());
        }
        return command;
    }


    private void applyCommand(BalanceExecutionResultCommand command, LedgerMovement movement) {
        for (BalanceExecutionResultCommand.CommandDetail detail : command.getDetails()) {
            apply(detail, movement);
        }
    }

    private void apply(BalanceExecutionResultCommand.CommandDetail cmd, LedgerMovement movement) {
        Account locked = accountRepository.lockById(cmd.getAccount().getId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + cmd.getAccount().getId()));
        BigDecimal ledger = locked.getLedgerBalance();
        BigDecimal available = locked.getAvailableBalance();
        if (cmd.getOperation() == BalanceOperation.ADD) {
            ledger = ledger.add(cmd.getAmount());
            available = available.add(cmd.getAmount());
        } else {
            if (!locked.isAllowNegative() && available.compareTo(cmd.getAmount()) < 0) {
                throw new BizException(MovementErrorResponse.MOV0403, "Insufficient available balance on account " + locked.getId());
            }
            ledger = ledger.subtract(cmd.getAmount());
            available = available.subtract(cmd.getAmount());
        }
        locked.setLedgerBalance(ledger);
        locked.setAvailableBalance(available);
        accountRepository.save(locked);
    }

    private void createLedgerEntries(LedgerMovement movement, BalanceExecutionResultCommand command) {
        for (BalanceExecutionResultCommand.CommandDetail cmd : command.getDetails()) {
            MovementDirection direction = cmd.getOperation() == BalanceOperation.ADD
                ? MovementDirection.CREDIT : MovementDirection.DEBIT;
            ledgerEntryRepository.save(new LedgerEntry(
                movement.getId(),
                String.valueOf(cmd.getAccount().getId()),
                cmd.getAmount(),
                direction,
                movement.getCurrency()));
        }
    }

    private Account resolveAccount(String idOrWalletRef, Currency currency) {
        if (idOrWalletRef == null || idOrWalletRef.isBlank()) {
            throw new BizException(AccountErrorResponse.ACC0400, "Account/wallet reference required");
        }
        try {
            Long id = Long.valueOf(idOrWalletRef);
            OptionalWalletAccount fromWallet = tryWallet(id, currency);
            if (fromWallet != null) {
                return fromWallet.account();
            }
            return accountRepository.findById(id)
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + id));
        } catch (NumberFormatException ex) {
            Wallet wallet = walletRepository.findByOwnerIdAndCurrency(idOrWalletRef, currency)
                .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                    "Wallet not found for " + idOrWalletRef + "/" + currency));
            return accountForWalletCurrency(wallet, currency);
        }
    }

    private OptionalWalletAccount tryWallet(Long walletId, Currency currency) {
        return walletRepository.findById(walletId).map(w -> {
            Account a = accountForWalletCurrency(w, currency);
            return new OptionalWalletAccount(a);
        }).orElse(null);
    }

    private Account accountForWalletCurrency(Wallet wallet, Currency currency) {
        Account primary = accountRepository.findById(wallet.getAccountId())
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404, "Account not found: " + wallet.getAccountId()));
        if (primary.getCurrency() == currency) {
            return primary;
        }
        return accountRepository.findByMainAccountAndCurrency(primary.getMainAccount(), currency)
            .orElseThrow(() -> new BizException(AccountErrorResponse.ACC0404,
                "Account currency not found for wallet " + wallet.getId() + " / " + currency));
    }

    private record OptionalWalletAccount(Account account) {}
}
