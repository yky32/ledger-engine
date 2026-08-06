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

/**
 * Port of the-wallet-ledger DtoWrapper (aliases to DtoMapper method names).
 */
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

    public static RecipientResponse getRecipientResponseDto(Recipient r) {
        return DtoMapper.toRecipient(r);
    }

    public static LinkedBankAccountResponse getLinkedBankAccountResponseDto(LinkedBankAccount a) {
        return DtoMapper.toLinkedBank(a);
    }

    public static PaymentMethodResponse getPaymentMethodResponseDto(PaymentMethod p) {
        return DtoMapper.toPaymentMethod(p);
    }

    public static FxRateResponse getFxRateResponseDto(FxRate r) {
        return DtoMapper.toFx(r);
    }

    public static ConfigurationResponse getConfigurationResponseDto(SystemConfiguration c) {
        return DtoMapper.toConfig(c);
    }

    public static VirtualAccountResponse getVirtualAccountResponseDto(VirtualAccount va,
                                                                      List<VirtualSubAccount> subs) {
        return DtoMapper.toVirtualAccount(va, subs);
    }
}
