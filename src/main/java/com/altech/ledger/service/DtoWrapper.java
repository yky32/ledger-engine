package com.altech.ledger.service;

import com.altech.ledger.entity.dto.ledger.LedgerDto.CoaType;
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
import com.altech.ledger.entity.dto.response.GetFxRateResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;
import com.altech.ledger.entity.dto.response.GetRuleExecutionResponseDto;
import com.altech.ledger.entity.dto.response.GetRuleResponseDto;
import com.altech.ledger.entity.dto.response.GetSystemConfigurationResponseDto;

/**
 * Static PO ↔ DTO mappers only. No business rules.
 */
public final class DtoWrapper {
    private DtoWrapper() {}

    // ---------- product: wallet onboarding ----------

    public static GetWalletOnboardResponseDto getWalletOnboardResponseDto(Wallet wallet, Account account) {
        return getWalletOnboardResponseDto(wallet, account,
            List.of(getWalletAccountResponseDto(account, null, true)));
    }

    public static GetWalletOnboardResponseDto getWalletOnboardResponseDto(
        Wallet wallet,
        Account primary,
        List<GetWalletAccountResponseDto> accounts
    ) {
        return GetWalletOnboardResponseDto.builder()
            .walletId(wallet.getId())
            .ownerId(wallet.getOwnerId())
            .settlementCurrency(wallet.getSettlementCurrency())
            .status(wallet.getStatus())
            .type(wallet.getType())
            .walletType(wallet.getWalletType())
            .name(wallet.getName())
            .account(getWalletAccountResponseDto(primary, null, true))
            .balance(getWalletBalanceResponseDto(primary))
            .accounts(accounts)
            .createDt(wallet.getCreateDt())
            .updateDt(wallet.getUpdateDt())
            .createBy(wallet.getCreatedBy())
            .updateBy(wallet.getUpdatedBy())
            .isActive(wallet.getIsActive())
            .build();
    }

    public static GetWalletAccountResponseDto getWalletAccountResponseDto(Account a) {
        return getWalletAccountResponseDto(a, null, null, null);
    }

    public static GetWalletAccountResponseDto getWalletAccountResponseDto(
        Account a,
        String refCode,
        Boolean primary
    ) {
        return getWalletAccountResponseDto(a, refCode, primary, null);
    }

    /**
     * @param refCode free-form product line code; null for primary
     * @param primary true when this is wallet.accountId
     * @param displayName optional label (COA leaf stays numeric in DB)
     */
    public static GetWalletAccountResponseDto getWalletAccountResponseDto(
        Account a,
        String refCode,
        Boolean primary,
        String displayName
    ) {
        CoaType coa = _coaType(a.getType());
        String name = displayName != null && !displayName.isBlank() ? displayName : a.getSubAccount();
        return GetWalletAccountResponseDto.builder()
            .id(a.getId())
            .fullNumber(a.getFullNumber())
            .name(name)
            .refCode(refCode)
            .primary(primary)
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

    private static CoaType _coaType(String type) {
        if (type == null) {
            return CoaType.LIABILITY;
        }
        return switch (type) {
            case "10" -> CoaType.ASSET;
            case "20" -> CoaType.LIABILITY;
            case "30" -> CoaType.EQUITY;
            case "40" -> CoaType.REVENUE;
            case "50" -> CoaType.EXPENSE;
            default -> {
                try {
                    yield CoaType.valueOf(type);
                } catch (Exception ex) {
                    yield CoaType.LIABILITY;
                }
            }
        };
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

    public static GetLedgerAccountResponseDto getLedgerAccountResponseDto(Account a) {
        return DtoMapper.toAccount(a);
    }

    public static GetLedgerWalletResponseDto getAccountBalanceResponseDto(Wallet w, List<Account> accounts) {
        return DtoMapper.toWallet(w, accounts);
    }

    public static GetLedgerMovementResponseDto getLedgerMovementResponseDto(LedgerMovement m) {
        return DtoMapper.toMovement(m);
    }

    public static GetRuleResponseDto getRuleResponseDto(Rule r) {
        return DtoMapper.toRule(r);
    }

    public static GetRuleExecutionResponseDto getRuleExecutionResponseDto(RuleExecution r) {
        return DtoMapper.toRuleExecution(r);
    }

    public static GetFxRateResponseDto getFxRateResponseDto(FxRate r) {
        return DtoMapper.toFx(r);
    }

    public static GetSystemConfigurationResponseDto getConfigurationResponseDto(SystemConfiguration c) {
        return DtoMapper.toConfig(c);
    }
}
