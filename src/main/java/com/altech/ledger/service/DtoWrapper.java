package com.altech.ledger.service;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.ingest.LedgerLegDto;
import com.altech.ledger.entity.dto.movement.MovementDto.MovementResponse;
import com.altech.ledger.entity.dto.response.GetAccountingRuleExecutionResponseDto;
import com.altech.ledger.entity.dto.response.GetAccountingRuleResponseDto;
import com.altech.ledger.entity.dto.response.GetCoaDictionaryResponseDto;
import com.altech.ledger.entity.dto.response.GetCoaProfileResponseDto;
import com.altech.ledger.entity.dto.response.GetDigestionRuleResponseDto;
import com.altech.ledger.entity.dto.response.GetFailedTransactionIngestResponseDto;
import com.altech.ledger.entity.dto.response.GetFxRateResponseDto;
import com.altech.ledger.entity.dto.response.GetIngestPolicyResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountBalanceResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerMovementResponseDto;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;
import com.altech.ledger.entity.dto.response.GetSystemConfigurationResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletAccountResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletBalanceResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletOnboardResponseDto;
import com.altech.ledger.entity.dto.response.GetWalletTierPolicyResponseDto;
import com.altech.ledger.entity.po.FxRate;
import com.altech.ledger.entity.po.accounting.AccountingRule;
import com.altech.ledger.entity.po.accounting.AccountingRuleExecution;
import com.altech.ledger.entity.po.coa.CoaDictionary;
import com.altech.ledger.entity.po.coa.CoaProfile;
import com.altech.ledger.entity.po.configuration.SystemConfiguration;
import com.altech.ledger.entity.po.digestion.DigestionRule;
import com.altech.ledger.entity.po.ingest.FailedTransactionIngest;
import com.altech.ledger.entity.po.ingest.IngestPolicy;
import com.altech.ledger.entity.po.ledger.Account;
import com.altech.ledger.entity.po.ledger.Wallet;
import com.altech.ledger.entity.po.log.LedgerEntry;
import com.altech.ledger.entity.po.log.LedgerMovement;
import com.altech.ledger.entity.po.wallet.WalletTierPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Static PO ↔ DTO mappers only. No business rules.
 */
public final class DtoWrapper {

    /** Book label: {@code 01A31658334-HKD}. */
    public static String bookDisplayName(String ownerId, Currency ccy) {
        return bookDisplayName(ownerId, ccy == null ? null : ccy.getIsoCode());
    }

    public static String bookDisplayName(String ownerId, String currency) {
        String id = ownerId == null ? "" : ownerId.trim();
        String iso = currency == null ? "" : currency.trim();
        if (!id.isEmpty() && !iso.isEmpty()) {
            return id + "-" + iso;
        }
        if (!iso.isEmpty()) {
            return iso;
        }
        return id.isEmpty() ? null : id;
    }

    // ---------- product: wallet onboarding ----------

    public static GetWalletOnboardResponseDto getWalletOnboardResponseDto(Wallet wallet, Account account) {
        return getWalletOnboardResponseDto(wallet, account,
            List.of(getWalletAccountResponseDto(
                account, null, true, bookDisplayName(wallet.getOwnerId(), account.getCurrency()))));
    }

    /** Wallet row for list-all (no accounts / balances). */
    public static GetWalletOnboardResponseDto getWalletListRowDto(Wallet wallet) {
        return GetWalletOnboardResponseDto.builder()
            .walletId(wallet.getId())
            .accountId(wallet.getAccountId())
            .ownerId(wallet.getOwnerId())
            .vanityCode(wallet.getVanityCode())
            .settlementCurrency(wallet.getSettlementCurrency())
            .status(wallet.getStatus())
            .type(wallet.getType())
            .walletType(wallet.getWalletType())
            .tier(wallet.getTier())
            .name(wallet.getName())
            .createDt(wallet.getCreateDt())
            .updateDt(wallet.getUpdateDt())
            .createBy(wallet.getCreatedBy())
            .updateBy(wallet.getUpdatedBy())
            .isActive(wallet.getIsActive())
            .build();
    }

    public static GetWalletOnboardResponseDto getWalletOnboardResponseDto(
        Wallet wallet,
        Account primary,
        List<GetWalletAccountResponseDto> accounts
    ) {
        return GetWalletOnboardResponseDto.builder()
            .walletId(wallet.getId())
            .accountId(wallet.getAccountId())
            .ownerId(wallet.getOwnerId())
            .vanityCode(wallet.getVanityCode())
            .settlementCurrency(wallet.getSettlementCurrency())
            .status(wallet.getStatus())
            .type(wallet.getType())
            .walletType(wallet.getWalletType())
            .tier(wallet.getTier())
            .name(wallet.getName())
            .account(getWalletAccountResponseDto(
                primary, null, true, bookDisplayName(wallet.getOwnerId(), primary.getCurrency())))
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
        String name = displayName != null && !displayName.isBlank()
            ? displayName
            : (a.getCurrency() != null ? a.getCurrency().getIsoCode() : a.getFullNumber());
        return GetWalletAccountResponseDto.builder()
            .id(a.getId())
            .walletId(a.getWalletId())
            .fullNumber(a.getFullNumber())
            .name(name)
            .refCode(refCode)
            .primary(primary)
            .entity(a.getEntity())
            .type(a.getType())
            .subType(a.getSubType())
            .mainAccount(a.getMainAccount())
            .buffer(a.getBuffer())
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

    // ---------- ledger / rules / fx / config ----------

    public static GetLedgerAccountResponseDto getLedgerAccountResponseDto(Account a) {
        return new GetLedgerAccountResponseDto(
            a.getId(), a.getFullNumber(), a.getEntity(), a.getType(), a.getSubType(),
            a.getMainAccount(), a.getBuffer(), a.getCurrency(),
            a.getLedgerBalance(), a.getAvailableBalance(), a.getStatus(),
            a.getCreateDt(), a.getUpdateDt());
    }

    public static GetLedgerWalletResponseDto getLedgerWalletResponseDto(Wallet w, List<Account> accounts) {
        return new GetLedgerWalletResponseDto(
            w.getId(),
            w.getAccountId(),
            w.getOwnerId(),
            w.getVanityCode(),
            w.getName(),
            w.getType(),
            w.getWalletType(),
            w.getStatus(),
            w.getSettlementCurrency(),
            w.getTier(),
            accounts.stream().map(DtoWrapper::getLedgerAccountResponseDto).toList(),
            w.getCreateDt(),
            w.getUpdateDt());
    }

    public static GetLedgerMovementResponseDto getLedgerMovementResponseDto(LedgerMovement m) {
        return new GetLedgerMovementResponseDto(
            m.getId(), m.getMovementKey(), m.getWalletId(), m.getTxnId(), m.getAlias(),
            m.getOriginatorId(), m.getTargetId(), m.getAmount(), m.getCurrency(),
            m.getOrderType(), m.getStatus(), m.getMode(), m.getType(),
            m.getRemarks(), m.getMetadata(), m.getComplianceContext(), m.getFiles(),
            m.getMainAccount(), m.getAssociatedLedgerMovementId(),
            m.getCreateDt(), m.getUpdateDt());
    }

    public static GetAccountingRuleResponseDto getAccountingRuleResponseDto(AccountingRule r) {
        return new GetAccountingRuleResponseDto(r.getId(), r.getName(), r.getDescription(), r.getDirection(),
            r.getMultiplier(), r.getTargetAccount(), r.getContent(), r.getCreateDt());
    }

    public static GetAccountingRuleExecutionResponseDto getAccountingRuleExecutionResponseDto(AccountingRuleExecution r) {
        return new GetAccountingRuleExecutionResponseDto(r.getId(), r.getName(), r.getDescription(),
            r.getOrderType(), r.getEventType(), r.getMetadata(), r.getCreateDt());
    }

    public static GetFxRateResponseDto getFxRateResponseDto(FxRate r) {
        return new GetFxRateResponseDto(r.getId(), r.getBase(), r.getTarget(), r.getRate(),
            r.getCreateDt(), r.getUpdateDt());
    }

    public static GetSystemConfigurationResponseDto getConfigurationResponseDto(SystemConfiguration c) {
        return new GetSystemConfigurationResponseDto(c.getId(), c.getName(), c.getTarget(), c.getScope(), c.getValue());
    }

    public static GetLedgerAccountBalanceResponseDto getLedgerAccountBalanceResponseDto(Account a) {
        return new GetLedgerAccountBalanceResponseDto(
            a.getId(), a.getCurrency(), a.getLedgerBalance(), a.getAvailableBalance(), null);
    }

    public static GetLedgerMovementResponseDto getLedgerMovementResponseDto(
        GetLedgerMovementResponseDto d,
        Currency currency
    ) {
        return new GetLedgerMovementResponseDto(
            d.id(), d.movementKey(), d.walletId(), d.txnId(), d.alias(),
            d.originatorId(), d.targetId(), d.amount(), currency,
            d.orderType(), d.status(), d.mode(), d.type(),
            d.remarks(), d.metadata(), d.complianceContext(), d.files(),
            d.mainAccount(), d.associatedLedgerMovementId(), d.createDt(), d.updateDt());
    }

    public static MovementResponse getMovementResponse(LedgerMovement m) {
        return getMovementResponse(getLedgerMovementResponseDto(m));
    }

    public static MovementResponse getMovementResponse(GetLedgerMovementResponseDto r) {
        return new MovementResponse(
            r.id(), r.movementKey(), r.walletId(), r.orderType(), r.status(), r.mode(),
            r.originatorId(), r.targetId(), r.amount(), r.currency(),
            r.createDt(), r.updateDt());
    }

    public static LedgerLegDto getLedgerLegDto(LedgerEntry e, Account book) {
        Long accountId = null;
        try {
            accountId = Long.valueOf(e.getTargetId());
        } catch (Exception ignored) {
            // leave null
        }
        return new LedgerLegDto(
            e.getId(),
            accountId,
            e.getDirection(),
            e.getAmount(),
            e.getCurrency(),
            book == null ? null : book.getFullNumber());
    }

    // ---------- ingest / digestion / coa ----------

    public static GetIngestPolicyResponseDto getIngestPolicyResponseDto(IngestPolicy p) {
        return GetIngestPolicyResponseDto.builder()
            .id(p.getId())
            .isEnabled(p.getIsEnabled())
            .isAutoCreateWallet(p.getIsAutoCreateWallet())
            .autoWalletSettlementCurrency(p.getAutoWalletSettlementCurrency())
            .autoWalletEnsureCurrency(p.getAutoWalletEnsureCurrency())
            .autoWalletAssociatedFrom(p.getAutoWalletAssociatedFrom())
            .autoWalletNamePrefix(p.getAutoWalletNamePrefix())
            .autoWalletCoaProfileCode(p.getAutoWalletCoaProfileCode())
            .entryFactors(p.getEntryFactors())
            .createDt(p.getCreateDt())
            .updateDt(p.getUpdateDt())
            .build();
    }

    public static GetWalletTierPolicyResponseDto getWalletTierPolicyResponseDto(WalletTierPolicy p) {
        return GetWalletTierPolicyResponseDto.builder()
            .id(p.getId())
            .isEnabled(p.getIsEnabled())
            .criterion(p.getCriterion() == null ? null : p.getCriterion().name())
            .currency(p.getCurrency())
            .bands(p.getBands())
            .createDt(p.getCreateDt())
            .updateDt(p.getUpdateDt())
            .build();
    }

    public static GetFailedTransactionIngestResponseDto getFailedTransactionIngestResponseDto(FailedTransactionIngest f) {
        return new GetFailedTransactionIngestResponseDto(
            f.getId(),
            f.getEventId(),
            f.getOwnerId(),
            f.getEventType(),
            f.getAmount(),
            f.getCurrency(),
            f.getOccurredAt(),
            f.getFailureCode(),
            f.getReason(),
            f.getStatus(),
            f.getRawPayload(),
            f.getCreateDt(),
            f.getUpdateDt()
        );
    }

    public static GetDigestionRuleResponseDto getDigestionRuleResponseDto(DigestionRule r) {
        return GetDigestionRuleResponseDto.builder()
            .id(r.getId())
            .code(r.getCode())
            .name(r.getName())
            .eventType(r.getEventType())
            .operation(r.getOperation())
            .isEnabled(r.getIsEnabled())
            .priority(r.getPriority())
            .minAmount(r.getMinAmount())
            .eligibleCurrencies(splitCsv(r.getEligibleCurrencies()))
            .eligibleMccs(splitCsv(r.getEligibleMccs()))
            .maxAgeDays(r.getMaxAgeDays())
            .resultCurrency(r.getResultCurrency())
            .formula(r.getFormula())
            .processType(r.getProcessType())
            .whenFactors(r.getWhenFactors())
            .createDt(r.getCreateDt())
            .updateDt(r.getUpdateDt())
            .build();
    }

    public static GetCoaDictionaryResponseDto getCoaDictionaryResponseDto(CoaDictionary r) {
        return GetCoaDictionaryResponseDto.builder()
            .id(r.getId())
            .kind(r.getKind() == null ? null : r.getKind().name())
            .code(r.getCode())
            .name(r.getName())
            .definition(r.getDefinition())
            .example(r.getExample())
            .side(r.getSide())
            .createDt(r.getCreateDt())
            .updateDt(r.getUpdateDt())
            .build();
    }

    public static GetCoaProfileResponseDto getCoaProfileResponseDto(CoaProfile p) {
        return GetCoaProfileResponseDto.builder()
            .id(p.getId())
            .code(p.getCode())
            .name(p.getName())
            .transactionCode(p.getTransactionCode())
            .isDefault(p.getIsDefault())
            .isEnabled(p.getIsEnabled())
            .entity(p.getEntity())
            .type(p.getType())
            .subType(p.getSubType())
            .buffer(p.getBuffer())
            .currency(p.getCurrency())
            .poolAllowNegative(p.getPoolAllowNegative())
            .walletId(p.getWalletId())
            .createDt(p.getCreateDt())
            .updateDt(p.getUpdateDt())
            .build();
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String p : csv.split(",")) {
            if (p != null && !p.isBlank()) {
                out.add(p.trim().toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }
}
