package com.altech.ledger.service;

import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
import com.altech.ledger.entity.dto.parity.FxRateDtos;
import com.altech.ledger.entity.dto.parity.LedgerAccountDtos;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.entity.dto.parity.SystemDtos;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletBalanceResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.entity.po.accounting.Rule;
import com.altech.ledger.entity.po.accounting.RuleExecution;
import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;

import java.util.List;

/**
 * Static PO ↔ DTO mappers only. No business rules.
 */
public final class DtoWrapper {
    private DtoWrapper() {}

    // ---------- product: wallet onboarding ----------

    public static GetWalletOnboardResponseDto getWalletOnboardResponseDto(Wallet wallet, Account account) {
        return GetWalletOnboardResponseDto.builder()
            .walletId(wallet.getId())
            .alias(wallet.getAlias())
            .ownerId(wallet.getOwnerId())
            .currency(wallet.getCurrency())
            .status(wallet.getStatus())
            .externalId(wallet.getExtIdentifier())
            .externalType(wallet.getExtType())
            .account(getWalletAccountResponseDto(account))
            .balance(getWalletBalanceResponseDto(account))
            .createDt(wallet.getCreateDt())
            .updateDt(wallet.getUpdateDt())
            .createBy(wallet.getCreatedBy())
            .updateBy(wallet.getUpdatedBy())
            .isActive(wallet.getIsActive())
            .build();
    }

    public static GetWalletAccountResponseDto getWalletAccountResponseDto(Account a) {
        CoaType coa;
        try {
            coa = CoaType.valueOf(a.getType());
        } catch (Exception ex) {
            coa = CoaType.LIABILITY;
        }
        return GetWalletAccountResponseDto.builder()
            .id(a.getId())
            .externalReference(a.getFullNumber())
            .name(a.getSubAccount())
            .type(coa)
            .currency(a.getCurrency())
            .status(a.getStatus())
            .allowNegative(a.isAllowNegative())
            .ledgerBalance(a.getLedgerBalance())
            .availableBalance(a.getAvailableBalance())
            .version(a.getVersion())
            .createDt(a.getCreateDt())
            .updateDt(a.getUpdateDt())
            .createBy(a.getCreatedBy())
            .updateBy(a.getUpdatedBy())
            .isActive(a.getIsActive())
            .build();
    }

    public static GetWalletBalanceResponseDto getWalletBalanceResponseDto(Account a) {
        return GetWalletBalanceResponseDto.builder()
            .accountId(a.getId())
            .currency(a.getCurrency())
            .ledgerBalance(a.getLedgerBalance())
            .availableBalance(a.getAvailableBalance())
            .build();
    }

    // ---------- parity API surface ----------

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
