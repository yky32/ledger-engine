package com.altech.ledger.entity.dto;

import com.altech.ledger.entity.enu.BalanceOperation;
import com.altech.ledger.entity.po.ledger.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory plan of account balance mutations produced when a movement is executed.
 * Each {@link CommandDetail} is one ADD/SUBTRACT leg applied under lock.
 */
@Getter
@Setter
@NoArgsConstructor
public class BalanceExecutionResultCommand {
    private List<CommandDetail> details = new ArrayList<>();

    public BalanceExecutionResultCommand(List<CommandDetail> details) {
        this.details = details == null ? new ArrayList<>() : details;
    }

    public void add(Account account, BigDecimal amount, BalanceOperation operation) {
        details.add(new CommandDetail(account, amount, operation));
    }

    /**
     * One balance change: account + amount + ADD or SUBTRACT.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandDetail {
        private Account account;
        private BigDecimal amount;
        private BalanceOperation operation;
    }
}
