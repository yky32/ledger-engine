package com.altech.ledger.usecase.ledger;

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
import com.altech.ledger.exception.LedgerException;
import com.altech.ledger.listener.intf.LedgerHandler;
import com.altech.ledger.repository.AccountRepository;
import com.altech.ledger.repository.LedgerEntryRepository;
import com.altech.ledger.repository.LedgerMovementRepository;
import com.altech.ledger.repository.WalletRepository;
import com.altech.ledger.service.MovementBus;
import com.altech.ledger.usecase.account.RuleExecutionUseCase;
import com.altech.ledger.usecase.setup.LinkedBankAccountUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Port of the-wallet-ledger LedgerMovementExecutionUseCase.
 * Structure: fetch rules → rulesExecution (command) → updateBalances → ledger entries.
 */
@Service
public class LedgerMovementExecutionUseCase implements LedgerHandler {
    private static final Logger log = LoggerFactory.getLogger(LedgerMovementExecutionUseCase.class);

    private final AccountRepository accounts;
    private final WalletRepository wallets;
    private final LedgerMovementRepository movements;
    private final LedgerEntryRepository entries;
    private final MovementBus movementBus;
    private final LinkedBankAccountUseCase linkedBankAccountUseCase;
    private final RuleExecutionUseCase ruleExecutionUseCase;

    public LedgerMovementExecutionUseCase(AccountRepository accounts, WalletRepository wallets,
                                          LedgerMovementRepository movements, LedgerEntryRepository entries,
                                          @Lazy MovementBus movementBus,
                                          LinkedBankAccountUseCase linkedBankAccountUseCase,
                                          RuleExecutionUseCase ruleExecutionUseCase) {
        this.accounts = accounts;
        this.wallets = wallets;
        this.movements = movements;
        this.entries = entries;
        this.movementBus = movementBus;
        this.linkedBankAccountUseCase = linkedBankAccountUseCase;
        this.ruleExecutionUseCase = ruleExecutionUseCase;
    }

    @Override
    @Transactional
    public void execute(LedgerMovementEvent event) {
        if (event.getMovementId() == null) {
            throw LedgerException.badRequest("MISSING_MOVEMENT_ID", "movementId required on event");
        }
        LedgerMovement movement = movements.findById(event.getMovementId())
            .orElseThrow(() -> LedgerException.notFound("Movement not found: " + event.getMovementId()));
        execute(movement);
    }

    @Override
    public List<Account> fetchAccounts(String identifier) {
        try {
            Long id = Long.valueOf(identifier);
            return accounts.findById(id).map(List::of).orElse(List.of());
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
            ruleExecutionUseCase.findByOrderType(movement.getOrderType()).ifPresent(re ->
                log.debug("RuleExecution found for {}: {}", movement.getOrderType(), re.name()));

            BalanceExecutionResultCommand command = rulesExecution(movement);
            applyCommand(command, movement);
            movement.setStatus(LedgerMovementStatus.SETTLED);
            movements.save(movement);
            createLedgerEntries(movement, command);
            movementBus.publishDone(movement);
            notification(MovementBus.toEvent(movement));
        } catch (RuntimeException ex) {
            log.error("movement execution failed id={}", movement.getId(), ex);
            movement.setStatus(LedgerMovementStatus.ERROR);
            movements.save(movement);
            throw ex;
        }
        return movement;
    }

    /**
     * Port of rulesExecution — builds BalanceExecutionResultCommand from OrderType.
     * Linked-bank deposit target → skip wallet credit (old behaviour).
     */
    public BalanceExecutionResultCommand rulesExecution(LedgerMovement movement) {
        BalanceExecutionResultCommand command = new BalanceExecutionResultCommand();
        BigDecimal amount = movement.getAmount();
        String currency = movement.getCurrency();

        switch (movement.getOrderType()) {
            case DEPOSIT, PAYMENT_LINK -> {
                if (isLinkedBankTarget(movement.getTargetId())) {
                    log.info("deposit target is linked bank {}; skip wallet credit", movement.getTargetId());
                    break;
                }
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
            default -> throw LedgerException.badRequest("UNSUPPORTED_ORDER_TYPE",
                "Unsupported order type: " + movement.getOrderType());
        }
        return command;
    }

    private boolean isLinkedBankTarget(String targetId) {
        if (targetId == null || targetId.isBlank()) return false;
        try {
            return linkedBankAccountUseCase.getOptionalById(Long.valueOf(targetId)).isPresent();
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void applyCommand(BalanceExecutionResultCommand command, LedgerMovement movement) {
        for (BalanceExecutionResultCommand.CommandDetail detail : command.getDetails()) {
            apply(detail, movement);
        }
    }

    private void apply(BalanceExecutionResultCommand.CommandDetail cmd, LedgerMovement movement) {
        Account locked = accounts.lockById(cmd.getAccount().getId())
            .orElseThrow(() -> LedgerException.notFound("Account not found: " + cmd.getAccount().getId()));
        BigDecimal ledger = locked.getLedgerBalance();
        BigDecimal available = locked.getAvailableBalance();
        if (cmd.getOperation() == BalanceOperation.ADD) {
            ledger = ledger.add(cmd.getAmount());
            available = available.add(cmd.getAmount());
        } else {
            if (!locked.isAllowNegative() && available.compareTo(cmd.getAmount()) < 0) {
                throw LedgerException.conflict("INSUFFICIENT_BALANCE",
                    "Insufficient available balance on account " + locked.getId());
            }
            ledger = ledger.subtract(cmd.getAmount());
            available = available.subtract(cmd.getAmount());
        }
        locked.setLedgerBalance(ledger);
        locked.setAvailableBalance(available);
        accounts.save(locked);
    }

    private void createLedgerEntries(LedgerMovement movement, BalanceExecutionResultCommand command) {
        for (BalanceExecutionResultCommand.CommandDetail cmd : command.getDetails()) {
            MovementDirection direction = cmd.getOperation() == BalanceOperation.ADD
                ? MovementDirection.CREDIT : MovementDirection.DEBIT;
            entries.save(new LedgerEntry(
                movement.getId(),
                String.valueOf(cmd.getAccount().getId()),
                cmd.getAmount(),
                direction,
                movement.getCurrency()));
        }
    }

    private Account resolveAccount(String idOrWalletRef, String currency) {
        if (idOrWalletRef == null || idOrWalletRef.isBlank()) {
            throw LedgerException.badRequest("MISSING_ACCOUNT", "Account/wallet reference required");
        }
        try {
            Long id = Long.valueOf(idOrWalletRef);
            OptionalWalletAccount fromWallet = tryWallet(id, currency);
            if (fromWallet != null) {
                return fromWallet.account();
            }
            return accounts.findById(id)
                .orElseThrow(() -> LedgerException.notFound("Account not found: " + id));
        } catch (NumberFormatException ex) {
            Wallet wallet = wallets.findByOwnerIdAndCurrency(idOrWalletRef, currency)
                .orElseThrow(() -> LedgerException.notFound(
                    "Wallet not found for " + idOrWalletRef + "/" + currency));
            return accountForWalletCurrency(wallet, currency);
        }
    }

    private OptionalWalletAccount tryWallet(Long walletId, String currency) {
        return wallets.findById(walletId).map(w -> {
            Account a = accountForWalletCurrency(w, currency);
            return new OptionalWalletAccount(a);
        }).orElse(null);
    }

    private Account accountForWalletCurrency(Wallet wallet, String currency) {
        Account primary = accounts.findById(wallet.getAccountId())
            .orElseThrow(() -> LedgerException.notFound("Account not found: " + wallet.getAccountId()));
        if (primary.getCurrency().equalsIgnoreCase(currency)) {
            return primary;
        }
        return accounts.findByMainAccountAndCurrency(primary.getMainAccount(), currency.toUpperCase())
            .orElseThrow(() -> LedgerException.notFound(
                "Account currency not found for wallet " + wallet.getId() + " / " + currency));
    }

    private record OptionalWalletAccount(Account account) {}
}
