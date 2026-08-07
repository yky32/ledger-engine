package com.altech.ledger.service;

import com.altech.ledger.entity.dto.parity.FxRateDtos;
import com.altech.ledger.entity.dto.parity.LedgerAccountDtos;
import com.altech.ledger.entity.dto.parity.LedgerMovementDtos;
import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;
import com.altech.ledger.entity.dto.parity.RuleDtos;
import com.altech.ledger.entity.dto.parity.SystemDtos;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.entity.po.accounting.Rule;
import com.altech.ledger.entity.po.accounting.RuleExecution;
import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerMovement;

import java.util.List;

/**
 * Parity-layer PO → DTO mapping. Prefer {@link DtoWrapper} at call sites.
 */
public final class DtoMapper {
    private DtoMapper() {}

    public static LedgerAccountDtos.Response toAccount(Account a) {
        return new LedgerAccountDtos.Response(
            a.getId(), a.getFullNumber(), a.getEntity(), a.getType(), a.getSubType(),
            a.getMainAccount(), a.getSubAccount(), a.getBuffer(), a.getCurrency(),
            a.getLedgerBalance(), a.getAvailableBalance(), a.getStatus(),
            a.getCreateDt(), a.getUpdateDt());
    }

    public static LedgerWalletDtos.WithBalancesResponse toWallet(Wallet w, List<Account> accounts) {
        return new LedgerWalletDtos.WithBalancesResponse(
            w.getId(), w.getAlias(), w.getAccountId(), w.getNickname(),
            w.getExtIdentifier(), w.getExtType(), w.getType(), w.getWalletType(),
            w.getStatus(), w.getOwnerId(), w.getCurrency(),
            accounts.stream().map(DtoMapper::toAccount).toList(),
            w.getCreateDt(), w.getUpdateDt());
    }

    public static LedgerMovementDtos.Response toMovement(LedgerMovement m) {
        return new LedgerMovementDtos.Response(
            m.getId(), m.getMovementKey(), m.getWalletId(), m.getTxnId(), m.getAlias(),
            m.getOriginatorId(), m.getTargetId(), m.getAmount(), m.getCurrency(),
            m.getOrderType(), m.getStatus(), m.getMode(), m.getType(),
            m.getRemarks(), m.getMetadata(), m.getComplianceContext(), m.getFiles(),
            m.getCreateDt(), m.getUpdateDt());
    }

    public static RuleDtos.Response toRule(Rule r) {
        return new RuleDtos.Response(r.getId(), r.getName(), r.getDescription(), r.getDirection(),
            r.getMultiplier(), r.getTargetAccount(), r.getContent(), r.getCreateDt());
    }

    public static RuleDtos.ExecutionResponse toRuleExecution(RuleExecution r) {
        return new RuleDtos.ExecutionResponse(r.getId(), r.getName(), r.getDescription(),
            r.getOrderType(), r.getMetadata(), r.getCreateDt());
    }

    public static FxRateDtos.Response toFx(FxRate r) {
        return new FxRateDtos.Response(r.getId(), r.getBase(), r.getTarget(), r.getRate(),
            r.getCreateDt(), r.getUpdateDt());
    }

    public static SystemDtos.ConfigurationResponse toConfig(SystemConfiguration c) {
        return new SystemDtos.ConfigurationResponse(c.getId(), c.getName(), c.getTarget(), c.getScope(), c.getValue());
    }
}
