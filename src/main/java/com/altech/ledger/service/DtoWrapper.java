package com.altech.ledger.service;

import com.altech.ledger.entity.dto.account.LedgerAccountDtos;
import com.altech.ledger.entity.dto.fx.FxRateDtos;
import com.altech.ledger.entity.dto.movement.LedgerMovementDtos;
import com.altech.ledger.entity.dto.rule.RuleDtos;
import com.altech.ledger.entity.dto.system.SystemDtos;
import com.altech.ledger.entity.dto.wallet.LedgerWalletDtos;
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

    public static LedgerAccountDtos.Response getLedgerAccountResponseDto(Account a) {
        return DtoMapper.toAccount(a);
    }

    public static LedgerWalletDtos.WithBalancesResponse getAccountBalanceResponseDto(Wallet w, List<Account> accounts) {
        return DtoMapper.toWallet(w, accounts);
    }

    public static LedgerMovementDtos.Response getLedgerMovementResponseDto(LedgerMovement m) {
        return DtoMapper.toMovement(m);
    }

    public static RuleDtos.Response getRuleResponseDto(Rule r) {
        return DtoMapper.toRule(r);
    }

    public static RuleDtos.ExecutionResponse getRuleExecutionResponseDto(RuleExecution r) {
        return DtoMapper.toRuleExecution(r);
    }

    public static FxRateDtos.Response getFxRateResponseDto(FxRate r) {
        return DtoMapper.toFx(r);
    }

    public static SystemDtos.ConfigurationResponse getConfigurationResponseDto(SystemConfiguration c) {
        return DtoMapper.toConfig(c);
    }
}
