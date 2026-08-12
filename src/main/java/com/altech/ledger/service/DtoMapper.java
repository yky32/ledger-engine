package com.altech.ledger.service;

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
 * Parity-layer PO → DTO mapping. Prefer {@link DtoWrapper} at call sites.
 */
public final class DtoMapper {
    private DtoMapper() {}

    public static GetLedgerAccountResponseDto toAccount(Account a) {
        return new GetLedgerAccountResponseDto(
            a.getId(), a.getFullNumber(), a.getEntity(), a.getType(), a.getSubType(),
            a.getMainAccount(), a.getSubAccount(), a.getBuffer(), a.getCurrency(),
            a.getLedgerBalance(), a.getAvailableBalance(), a.getStatus(),
            a.getCreateDt(), a.getUpdateDt());
    }

    public static GetLedgerWalletResponseDto toWallet(Wallet w, List<Account> accounts) {
        return new GetLedgerWalletResponseDto(
            w.getId(),
            w.getAccountId(),
            w.getOwnerId(),
            w.getOwnerId(), // associatedIdentifier API alias
            w.getName(),
            w.getStatus(),
            w.getSettlementCurrency(),
            accounts.stream().map(DtoMapper::toAccount).toList(),
            w.getCreateDt(),
            w.getUpdateDt());
    }

    public static GetLedgerMovementResponseDto toMovement(LedgerMovement m) {
        return new GetLedgerMovementResponseDto(
            m.getId(), m.getMovementKey(), m.getWalletId(), m.getTxnId(), m.getAlias(),
            m.getOriginatorId(), m.getTargetId(), m.getAmount(), m.getCurrency(),
            m.getOrderType(), m.getStatus(), m.getMode(), m.getType(),
            m.getRemarks(), m.getMetadata(), m.getComplianceContext(), m.getFiles(),
            m.getCreateDt(), m.getUpdateDt());
    }

    public static GetRuleResponseDto toRule(Rule r) {
        return new GetRuleResponseDto(r.getId(), r.getName(), r.getDescription(), r.getDirection(),
            r.getMultiplier(), r.getTargetAccount(), r.getContent(), r.getCreateDt());
    }

    public static GetRuleExecutionResponseDto toRuleExecution(RuleExecution r) {
        return new GetRuleExecutionResponseDto(r.getId(), r.getName(), r.getDescription(),
            r.getOrderType(), r.getMetadata(), r.getCreateDt());
    }

    public static GetFxRateResponseDto toFx(FxRate r) {
        return new GetFxRateResponseDto(r.getId(), r.getBase(), r.getTarget(), r.getRate(),
            r.getCreateDt(), r.getUpdateDt());
    }

    public static GetSystemConfigurationResponseDto toConfig(SystemConfiguration c) {
        return new GetSystemConfigurationResponseDto(c.getId(), c.getName(), c.getTarget(), c.getScope(), c.getValue());
    }
}
