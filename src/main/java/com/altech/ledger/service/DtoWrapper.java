package com.altech.ledger.service;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.entity.po.accounting.Rule;
import com.altech.ledger.entity.po.accounting.RuleExecution;
import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;

import java.util.List;

/** Thin aliases over {@link DtoMapper}. */
public final class DtoWrapper {
    private DtoWrapper() {}

    public static AccountResponse getLedgerAccountResponseDto(Account a) {
        return DtoMapper.toAccount(a);
    }

    public static WalletWithBalancesResponse getAccountBalanceResponseDto(Wallet w, List<Account> accounts) {
        return DtoMapper.toWallet(w, accounts);
    }

    public static MovementResponse getLedgerMovementResponseDto(LedgerMovement m) {
        return DtoMapper.toMovement(m);
    }

    public static RuleResponse getRuleResponseDto(Rule r) {
        return DtoMapper.toRule(r);
    }

    public static RuleExecutionResponse getRuleExecutionResponseDto(RuleExecution r) {
        return DtoMapper.toRuleExecution(r);
    }

    public static FxRateResponse getFxRateResponseDto(FxRate r) {
        return DtoMapper.toFx(r);
    }

    public static ConfigurationResponse getConfigurationResponseDto(SystemConfiguration c) {
        return DtoMapper.toConfig(c);
    }
}
