package com.altech.ledger.usecase.rule;

import com.altech.core.exception.BizException;
import com.altech.core.utils.JSONUtil;
import com.altech.ledger.entity.dto.request.AccountingRuleRefDto;
import com.altech.ledger.entity.json_context.AccountingRuleExecutionMetadata;
import com.altech.ledger.entity.po.accounting.AccountingRuleExecution;
import com.altech.ledger.exception.response.AccountErrorResponse;
import com.altech.ledger.exception.response.AccountingRuleErrorResponse;
import com.altech.ledger.repository.AccountingRuleExecutionRepository;
import com.altech.ledger.repository.AccountingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared write helpers: encode walk JSON, validate leg ids, bind eventType (one combo live).
 */
@Component
@RequiredArgsConstructor
class AccountingRuleExecutionWriter {
    private final AccountingRuleRepository accountingRuleRepository;
    private final AccountingRuleExecutionRepository accountingRuleExecutionRepository;

    String encodeMetadata(List<AccountingRuleRefDto> rules, String rawMetadata) {
        if (rules != null && !rules.isEmpty()) {
            List<AccountingRuleExecutionMetadata.Detail> details = new ArrayList<>();
            int i = 1;
            for (AccountingRuleRefDto ref : rules) {
                if (ref == null || ref.id() == null || ref.id().isBlank()) {
                    continue;
                }
                Long id;
                try {
                    id = Long.valueOf(ref.id().trim());
                } catch (NumberFormatException ex) {
                    throw new BizException(AccountErrorResponse.ACC0400, "AccountingRule id not numeric: " + ref.id());
                }
                if (!accountingRuleRepository.existsById(id)) {
                    throw new BizException(AccountingRuleErrorResponse.RUL0404, "AccountingRule not found: " + id);
                }
                int seq = ref.seq() == null ? i : ref.seq();
                details.add(new AccountingRuleExecutionMetadata.Detail(String.valueOf(id), seq));
                i++;
            }
            if (details.isEmpty()) {
                throw new BizException(AccountErrorResponse.ACC0400, "rules must include at least one AccountingRule id");
            }
            return JSONUtil.writeValue(new AccountingRuleExecutionMetadata(details));
        }
        return rawMetadata;
    }

    /** One live combination per eventType. Previous binding is cleared. */
    void bindEventType(AccountingRuleExecution target, String eventType) {
        String et = eventType == null ? null : eventType.trim().toUpperCase(Locale.ROOT);
        if (et != null && et.isEmpty()) {
            et = null;
        }
        if (et != null) {
            accountingRuleExecutionRepository.findByEventType(et).ifPresent(other -> {
                if (!other.getId().equals(target.getId())) {
                    other.setEventType(null);
                    accountingRuleExecutionRepository.saveAndFlush(other);
                }
            });
        }
        target.setEventType(et);
    }
}
