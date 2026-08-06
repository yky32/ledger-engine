package com.altech.ledger.listener.intf;

import com.altech.ledger.entity.dto.event.LedgerMovementEvent;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.po.ledger.Account;

import java.util.List;

/**
 * Port of the-wallet-ledger LedgerHandler.
 */
public interface LedgerHandler {
    List<Account> fetchAccounts(String identifier);

    void execute(LedgerMovementEvent event);

    void updateBalances(LedgerMovementEvent event);

    void notification(LedgerMovementEvent event);

    default void validateRules(OrderType orderType) {
        // optional rule-execution presence check
    }
}
