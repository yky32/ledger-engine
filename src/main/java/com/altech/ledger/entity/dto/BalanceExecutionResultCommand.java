package com.altech.ledger.entity.dto;

import com.altech.ledger.entity.enu.BalanceOperation;
import com.altech.ledger.entity.po.ledger.Account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Port of the-wallet-ledger BalanceExecutionResultCommand.
 */
public class BalanceExecutionResultCommand {
    private List<CommandDetail> details = new ArrayList<>();

    public BalanceExecutionResultCommand() {}

    public BalanceExecutionResultCommand(List<CommandDetail> details) {
        this.details = details == null ? new ArrayList<>() : details;
    }

    public List<CommandDetail> getDetails() {
        return details;
    }

    public void setDetails(List<CommandDetail> details) {
        this.details = details;
    }

    public void add(Account account, BigDecimal amount, BalanceOperation operation) {
        details.add(new CommandDetail(account, amount, operation));
    }

    public static class CommandDetail {
        private Account account;
        private BigDecimal amount;
        private BalanceOperation operation;

        public CommandDetail() {}

        public CommandDetail(Account account, BigDecimal amount, BalanceOperation operation) {
            this.account = account;
            this.amount = amount;
            this.operation = operation;
        }

        public Account getAccount() { return account; }
        public void setAccount(Account account) { this.account = account; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BalanceOperation getOperation() { return operation; }
        public void setOperation(BalanceOperation operation) { this.operation = operation; }
    }
}
