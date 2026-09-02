package com.altech.ledger.usecase.rule;

import com.altech.core.constant.enu.Currency;
import com.altech.core.utils.JSONUtil;
import com.altech.ledger.entity.enu.MovementDirection;
import com.altech.ledger.entity.enu.OrderType;
import com.altech.ledger.entity.json_context.AccountingRuleExecutionMetadata;
import com.altech.ledger.entity.po.accounting.AccountingRule;
import com.altech.ledger.entity.po.accounting.AccountingRuleExecution;
import com.altech.ledger.repository.AccountingRuleRepository;
import com.altech.ledger.repository.AccountingRuleExecutionRepository;
import com.altech.ledger.repository.CoaProfileRepository;
import com.altech.ledger.usecase.coa.CoaProfileUseCase;
import com.altech.ledger.usecase.coa.HouseBooksUseCase;
import com.altech.ledger.entity.dto.request.CreateCoaProfileRequestDto;
import com.altech.ledger.entity.dto.response.GetAccountingRulesBundleDto;
import com.altech.ledger.service.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Default posting sequences + lookup.
 * COA is the chart; this catalog is the CR/DR walk that produces legs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountingRuleCatalogUseCase {
    public static final String CUSTOMER_CUST_HKD = "CUSTOMER_CUST_HKD";
    public static final String CUSTOMER_CUST_LP = "CUSTOMER_CUST_LP";
    public static final String HOUSE_CC_OP_HKD = "HOUSE_CC_OP_HKD";
    public static final String HOUSE_CC_OP_LP = "HOUSE_CC_OP_LP";

    public static final String SEQ_EARN_LP = "EARN_LP";
    public static final String SEQ_EARN_HKD = "EARN_HKD";
    public static final String SEQ_CC_TXN_LP = "CC_TXN_LP";

    private final AccountingRuleRepository accountingRuleRepository;
    private final AccountingRuleExecutionRepository accountingRuleExecutionRepository;
    private final CoaProfileRepository coaProfileRepository;
    private final CoaProfileUseCase coaProfileUseCase;
    private final HouseBooksUseCase houseBooksUseCase;

    @Transactional
    public void ensureDefault() {
        houseBooksUseCase.ensure(HouseBooksUseCase.DEFAULT_OWNER);
        _seedCustomerCustodian(CUSTOMER_CUST_HKD, "Customer Custodian HKD", "HKD");
        _seedCustomerCustodian(CUSTOMER_CUST_LP, "Customer Custodian LP", "LP");

        // Double-entry is same-currency first: LP earn DR/CR LP; HKD earn DR/CR HKD.
        AccountingRule drOpHkd = _seedRule("TXN_DR_OP_HKD",
            "Debit operating HKD 01-02-01", MovementDirection.DEBIT, HOUSE_CC_OP_HKD);
        AccountingRule drOpLp = _seedRule("TXN_DR_OP_LP",
            "Debit operating LP 01-02-01", MovementDirection.DEBIT, HOUSE_CC_OP_LP);
        AccountingRule crCustHkd = _seedRule("TXN_CR_CUST_HKD",
            "Credit customer reward HKD 01-01-01", MovementDirection.CREDIT, CUSTOMER_CUST_HKD);
        AccountingRule crCustLp = _seedRule("TXN_CR_CUST_LP",
            "Credit customer reward LP 01-01-01", MovementDirection.CREDIT, CUSTOMER_CUST_LP);

        _seedSequence(SEQ_EARN_HKD, "Transaction → HKD (same currency)", OrderType.EARN, "CC_TXN_HKD",
            List.of(drOpHkd, crCustHkd), true);
        _seedSequence(SEQ_CC_TXN_LP, "Transaction → LP (same currency)", OrderType.EARN, "CC_TXN_LP",
            List.of(drOpLp, crCustLp), true);
        _seedSequence(SEQ_EARN_LP, "Default EARN = Transaction → LP (same currency)", OrderType.EARN, null,
            List.of(drOpLp, crCustLp), true);
    }

    /** createIfNotFound UA movement sequences, then list what is in DB. */
    @Transactional
    public GetAccountingRulesBundleDto ensureAndList() {
        ensureDefault();
        return new GetAccountingRulesBundleDto(
            accountingRuleRepository.findAll().stream().map(DtoMapper::toAccountingRule).toList(),
            accountingRuleExecutionRepository.findAll().stream().map(DtoMapper::toAccountingRuleExecution).toList());
    }

    /**
     * 1. Bound combo for webhook {@code eventType} ({@code CC_TXN} / {@code CC_CIP} /
     *    {@code CC_SIP} / {@code LN_TXN}, or a legacy ccy-suffixed bind).
     * 2. Else Brain {@code resultCurrency} → cashback HKD ({@code EARN_HKD} / {@code CC_TXN_HKD})
     *    or loyalty LP ({@code EARN_LP} / {@code CC_TXN_LP}).
     * 3. Else default EARN (LP).
     */
    @Transactional(readOnly = true)
    public Optional<AccountingRuleExecution> findSequence(String eventType, OrderType orderType) {
        return findSequence(eventType, orderType, null);
    }

    @Transactional(readOnly = true)
    public Optional<AccountingRuleExecution> findSequence(
        String eventType,
        OrderType orderType,
        Currency reward
    ) {
        if (eventType != null && !eventType.isBlank()) {
            String et = eventType.trim().toUpperCase(Locale.ROOT);
            Optional<AccountingRuleExecution> byEvent = accountingRuleExecutionRepository.findByEventType(et);
            if (byEvent.isPresent()) {
                return byEvent;
            }
        }
        if (reward != null) {
            Optional<AccountingRuleExecution> byReward = accountingRuleExecutionRepository.findByEventType(
                "CC_TXN_" + reward.getIsoCode());
            if (byReward.isPresent()) {
                return byReward;
            }
            Optional<AccountingRuleExecution> byName = accountingRuleExecutionRepository.findByName(
                "EARN_" + reward.getIsoCode());
            if (byName.isPresent()) {
                return byName;
            }
        }
        if (orderType != null) {
            return accountingRuleExecutionRepository.findFirstByOrderTypeAndEventTypeIsNull(orderType);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<AccountingRule> loadRules(AccountingRuleExecution execution) {
        if (execution == null || execution.getMetadata() == null || execution.getMetadata().isBlank()) {
            return List.of();
        }
        AccountingRuleExecutionMetadata meta;
        try {
            meta = JSONUtil.readValue(execution.getMetadata(), AccountingRuleExecutionMetadata.class);
        } catch (RuntimeException ex) {
            log.warn("AccountingRuleExecution metadata not JSON id={}", execution.getId());
            return List.of();
        }
        if (meta == null || meta.rules() == null || meta.rules().isEmpty()) {
            return List.of();
        }
        List<AccountingRuleExecutionMetadata.Detail> details = new ArrayList<>(meta.rules());
        details.sort(Comparator.comparing(d -> d.seq() == null ? Integer.MAX_VALUE : d.seq()));
        List<AccountingRule> out = new ArrayList<>();
        for (AccountingRuleExecutionMetadata.Detail d : details) {
            if (d.id() == null || d.id().isBlank()) {
                continue;
            }
            try {
                Long id = Long.valueOf(d.id());
                accountingRuleRepository.findById(id).ifPresent(out::add);
            } catch (NumberFormatException ignored) {
                log.warn("AccountingRule id not numeric in sequence {}: {}", execution.getName(), d.id());
            }
        }
        return out;
    }

    private void _seedCustomerCustodian(String code, String name, String currency) {
        _renameCoaCode("MEMBER_CUST_" + currency, code, name);
        if (coaProfileRepository.existsByCode(code)) {
            return;
        }
        coaProfileUseCase.create(new CreateCoaProfileRequestDto(
            code, name, null, false, true,
            "01", "01", "01", "00", currency, false, null));
    }

    /** MEMBER_CUST_* → CUSTOMER_CUST_* (wording). No-op if already renamed. */
    private void _renameCoaCode(String from, String to, String name) {
        if (from == null || to == null || from.equalsIgnoreCase(to)) {
            return;
        }
        if (coaProfileRepository.existsByCode(to)) {
            return;
        }
        coaProfileRepository.findByCode(from).ifPresent(p -> {
            p.setCode(to);
            if (p.getTransactionCode() == null || from.equalsIgnoreCase(p.getTransactionCode())) {
                p.setTransactionCode(to);
            }
            if (name != null && !name.isBlank()) {
                p.setName(name);
            }
            coaProfileRepository.save(p);
            log.info("Renamed COA {} → {}", from, to);
        });
    }

    private AccountingRule _seedRule(String name, String description, MovementDirection direction, String coaCode) {
        return accountingRuleRepository.findByName(name)
            .map(existing -> {
                boolean dirty = false;
                if (direction != existing.getDirection()) {
                    existing.setDirection(direction);
                    dirty = true;
                }
                if (!coaCode.equals(existing.getTargetAccount())) {
                    existing.setTargetAccount(coaCode);
                    dirty = true;
                }
                if (existing.getMultiplier() == null || existing.getMultiplier().compareTo(BigDecimal.ONE) != 0) {
                    existing.setMultiplier(BigDecimal.ONE);
                    dirty = true;
                }
                if (dirty) {
                    existing.setDescription(description);
                    return accountingRuleRepository.save(existing);
                }
                return existing;
            })
            .orElseGet(() -> {
                AccountingRule r = new AccountingRule();
                r.setName(name);
                r.setDescription(description);
                r.setDirection(direction);
                r.setMultiplier(BigDecimal.ONE);
                r.setTargetAccount(coaCode);
                r.setIsActive(true);
                return accountingRuleRepository.save(r);
            });
    }

    private void _seedSequence(
        String name,
        String description,
        OrderType orderType,
        String eventType,
        List<AccountingRule> rules,
        boolean realignLegs
    ) {
        List<AccountingRuleExecutionMetadata.Detail> details = new ArrayList<>();
        int seq = 1;
        for (AccountingRule r : rules) {
            details.add(new AccountingRuleExecutionMetadata.Detail(String.valueOf(r.getId()), seq++));
        }
        String metadata = JSONUtil.writeValue(new AccountingRuleExecutionMetadata(details));
        Optional<AccountingRuleExecution> existing = accountingRuleExecutionRepository.findByName(name);
        if (existing.isPresent()) {
            if (realignLegs && !_sameRuleIds(existing.get(), rules)) {
                AccountingRuleExecution ex = existing.get();
                ex.setDescription(description);
                ex.setMetadata(metadata);
                accountingRuleExecutionRepository.save(ex);
                log.info("Realigned posting sequence {} legs={}", name, rules.size());
            }
            return;
        }
        AccountingRuleExecution ex = new AccountingRuleExecution();
        ex.setName(name);
        ex.setDescription(description);
        ex.setOrderType(orderType);
        ex.setEventType(eventType);
        ex.setMetadata(metadata);
        ex.setIsActive(true);
        accountingRuleExecutionRepository.save(ex);
        log.info("Seeded posting sequence {} eventType={} legs={}", name, eventType, rules.size());
    }

    private boolean _sameRuleIds(AccountingRuleExecution execution, List<AccountingRule> rules) {
        List<AccountingRule> loaded = loadRules(execution);
        if (loaded.size() != rules.size()) {
            return false;
        }
        for (int i = 0; i < rules.size(); i++) {
            Long a = loaded.get(i).getId();
            Long b = rules.get(i).getId();
            if (a == null || b == null || !a.equals(b)) {
                return false;
            }
        }
        return true;
    }
}
