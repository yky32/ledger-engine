package com.altech.ledger.usecase.rule;

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
    public static final String MEMBER_CUST_HKD = "MEMBER_CUST_HKD";
    public static final String MEMBER_CUST_LP = "MEMBER_CUST_LP";
    public static final String HOUSE_CC_OP_HKD = "HOUSE_CC_OP_HKD";

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
        _seedMemberCustodian(MEMBER_CUST_HKD, "Member Custodian HKD", "HKD");
        _seedMemberCustodian(MEMBER_CUST_LP, "Member Custodian LP", "LP");

        // UA movement example: house DR is always Operating HKD 01-02-01.
        AccountingRule drOpHkd = _seedRule("TXN_DR_OP_HKD",
            "Debit operating HKD 01-02-01", MovementDirection.DEBIT, HOUSE_CC_OP_HKD);
        AccountingRule crCustHkd = _seedRule("TXN_CR_CUST_HKD",
            "Credit customer reward HKD 01-01-01", MovementDirection.CREDIT, MEMBER_CUST_HKD);
        AccountingRule crCustLp = _seedRule("TXN_CR_CUST_LP",
            "Credit customer reward LP 01-01-01", MovementDirection.CREDIT, MEMBER_CUST_LP);

        _seedSequence(SEQ_EARN_HKD, "Transaction → HKD", OrderType.EARN, "CC_TXN_HKD",
            List.of(drOpHkd, crCustHkd));
        _seedSequence(SEQ_CC_TXN_LP, "Transaction → LP (house DR still HKD)", OrderType.EARN, "CC_TXN_LP",
            List.of(drOpHkd, crCustLp));
        _seedSequence(SEQ_EARN_LP, "Default EARN = Transaction → LP", OrderType.EARN, null,
            List.of(drOpHkd, crCustLp));
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
     * eventType first, then default sequence for the orderType (eventType null).
     */
    @Transactional(readOnly = true)
    public Optional<AccountingRuleExecution> findSequence(String eventType, OrderType orderType) {
        if (eventType != null && !eventType.isBlank()) {
            String et = eventType.trim().toUpperCase(Locale.ROOT);
            Optional<AccountingRuleExecution> byEvent = accountingRuleExecutionRepository.findByEventType(et);
            if (byEvent.isPresent()) {
                return byEvent;
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

    private void _seedMemberCustodian(String code, String name, String currency) {
        if (coaProfileRepository.existsByCode(code)) {
            return;
        }
        coaProfileUseCase.create(new CreateCoaProfileRequestDto(
            code, name, null, false, true,
            "01", "01", "01", "00", currency, false, null));
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
        List<AccountingRule> rules
    ) {
        List<AccountingRuleExecutionMetadata.Detail> details = new ArrayList<>();
        int seq = 1;
        for (AccountingRule r : rules) {
            details.add(new AccountingRuleExecutionMetadata.Detail(String.valueOf(r.getId()), seq++));
        }
        if (accountingRuleExecutionRepository.findByName(name).isPresent()) {
            return;
        }
        String metadata = JSONUtil.writeValue(new AccountingRuleExecutionMetadata(details));
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
}
