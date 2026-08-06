package com.altech.ledger.service;

import com.altech.ledger.entity.dto.parity.ParityDtos.*;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.entity.po.LinkedBankAccount;
import com.altech.ledger.entity.po.Recipient;
import com.altech.ledger.entity.po.accounting.Rule;
import com.altech.ledger.entity.po.accounting.RuleExecution;
import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import com.altech.ledger.entity.po.ledger.*;
import com.altech.ledger.entity.po.log.LedgerMovement;

import java.util.List;

public final class DtoMapper {
    private DtoMapper() {}

    public static AccountResponse toAccount(Account a) {
        return new AccountResponse(
            a.getId(), a.getFullNumber(), a.getEntity(), a.getType(), a.getSubType(),
            a.getMainAccount(), a.getSubAccount(), a.getBuffer(), a.getCurrency(),
            a.getLedgerBalance(), a.getAvailableBalance(), a.getStatus(),
            a.getCreateDt(), a.getUpdateDt());
    }

    public static WalletWithBalancesResponse toWallet(Wallet w, List<Account> accounts) {
        return new WalletWithBalancesResponse(
            w.getId(), w.getAlias(), w.getAccountId(), w.getNickname(),
            w.getExtIdentifier(), w.getExtType(), w.getType(), w.getWalletType(),
            w.getStatus(), w.getOwnerId(), w.getCurrency(),
            accounts.stream().map(DtoMapper::toAccount).toList(),
            w.getCreateDt(), w.getUpdateDt());
    }

    public static MovementResponse toMovement(LedgerMovement m) {
        return new MovementResponse(
            m.getId(), m.getMovementKey(), m.getWalletId(), m.getTxnId(), m.getAlias(),
            m.getOriginatorId(), m.getTargetId(), m.getAmount(), m.getCurrency(),
            m.getOrderType(), m.getStatus(), m.getMode(), m.getType(),
            m.getRemarks(), m.getMetadata(), m.getComplianceContext(), m.getFiles(),
            m.getJournalTransactionId(), m.getCreateDt(), m.getUpdateDt());
    }

    public static RuleResponse toRule(Rule r) {
        return new RuleResponse(r.getId(), r.getName(), r.getDescription(), r.getDirection(),
            r.getMultiplier(), r.getTargetAccount(), r.getContent(), r.getCreateDt());
    }

    public static RuleExecutionResponse toRuleExecution(RuleExecution r) {
        return new RuleExecutionResponse(r.getId(), r.getName(), r.getDescription(),
            r.getOrderType(), r.getMetadata(), r.getCreateDt());
    }

    public static RecipientResponse toRecipient(Recipient r) {
        return new RecipientResponse(r.getId(), r.getTenantId(), r.getTransferChannel(),
            r.getStatus(), r.getMetadata(), r.getCreateDt());
    }

    public static LinkedBankAccountResponse toLinkedBank(LinkedBankAccount a) {
        return new LinkedBankAccountResponse(a.getId(), a.getTenantId(), a.getStatus(),
            a.getMetadata(), a.getCreateDt());
    }

    public static PaymentMethodResponse toPaymentMethod(PaymentMethod p) {
        return new PaymentMethodResponse(p.getId(), p.getWalletId(), p.getType(), p.getStatus(),
            p.getMetadata(), p.getTokenizationValue(), p.getTokenizationProvider(),
            p.getHash(), p.getCreateDt());
    }

    public static VirtualSubAccountResponse toVirtualSub(VirtualSubAccount s) {
        return new VirtualSubAccountResponse(s.getId(), s.getCurrency(), s.getStatus(),
            s.getLedgerBalance(), s.getAvailableBalance());
    }

    public static VirtualAccountResponse toVirtualAccount(VirtualAccount va, List<VirtualSubAccount> subs) {
        return new VirtualAccountResponse(va.getId(), va.getExtIdentifier(), va.getExtType(),
            va.getStatus(), va.getNickName(), va.getType(), va.getMetadata(),
            subs.stream().map(DtoMapper::toVirtualSub).toList(), va.getCreateDt());
    }

    public static VirtualAccountApplicationResponse toVaApp(VirtualAccountApplication app) {
        return new VirtualAccountApplicationResponse(
            app.getId(), app.getStatus(), app.getType(), app.getExtIdentifier(), app.getExtType(),
            app.getRemark(), app.getMetadata(),
            app.getVirtualAccount() == null ? null : app.getVirtualAccount().getId(),
            app.getCreateDt());
    }

    public static FxRateResponse toFx(FxRate r) {
        return new FxRateResponse(r.getId(), r.getBase(), r.getTarget(), r.getRate(),
            r.getCreateDt(), r.getUpdateDt());
    }

    public static ConfigurationResponse toConfig(SystemConfiguration c) {
        return new ConfigurationResponse(c.getId(), c.getName(), c.getTarget(), c.getScope(), c.getValue());
    }
}
