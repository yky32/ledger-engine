package com.altech.ledger.usecase.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.dto.posting.PostingRecipe;
import com.altech.ledger.entity.enu.PostingAtom;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * UA sheet use-cases → recipes. Product speaks codes; Finance COA stays behind roles.
 */
@Component
public class PostingRecipeCatalog {

    private final Map<String, PostingRecipe> byCode = new LinkedHashMap<>();

    public PostingRecipeCatalog() {
        // —— CC Transaction family ——
        put("CC_TXN_HKD", "UA_CC", List.of(PostingAtom.CREDIT_REWARD), Currency.HKD);
        put("CC_TXN_LP", "UA_CC", List.of(PostingAtom.CREDIT_REWARD), Currency.LP);
        put("CC_TXN_HKD_REDEEM", "UA_CC",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.REDEEM), Currency.HKD);
        put("CC_TXN_HKD_CASHBACK", "UA_CC",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CASHBACK), Currency.HKD);
        put("CC_TXN_LP_REDEEM", "UA_CC",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.REDEEM), Currency.LP);
        put("CC_TXN_LP_CASHBACK", "UA_CC",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CASHBACK), Currency.LP);
        put("CC_TXN_HKD_TO_LP", "UA_CC",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CONVERT_HKD_TO_LP), Currency.HKD);
        put("CC_TXN_HKD_LP_REDEEM", "UA_CC",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CONVERT_HKD_TO_LP, PostingAtom.REDEEM),
            Currency.HKD);
        put("CC_TXN_HKD_LP_CASHBACK", "UA_CC",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CONVERT_HKD_TO_LP, PostingAtom.CASHBACK),
            Currency.HKD);

        // —— Loan / Load DD family (Load treated as Loan alias) ——
        put("LOAN_DD_HKD", "UA_LOAN", List.of(PostingAtom.CREDIT_REWARD), Currency.HKD);
        put("LOAN_DD_LP", "UA_LOAN", List.of(PostingAtom.CREDIT_REWARD), Currency.LP);
        put("LOAN_DD_HKD_REDEEM", "UA_LOAN",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.REDEEM), Currency.HKD);
        put("LOAN_DD_HKD_CASHBACK", "UA_LOAN",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CASHBACK), Currency.HKD);
        put("LOAN_DD_LP_REDEEM", "UA_LOAN",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.REDEEM), Currency.LP);
        put("LOAN_DD_LP_CASHBACK", "UA_LOAN",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CASHBACK), Currency.LP);
        put("LOAN_DD_HKD_TO_LP", "UA_LOAN",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CONVERT_HKD_TO_LP), Currency.HKD);
        put("LOAN_DD_HKD_LP_REDEEM", "UA_LOAN",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CONVERT_HKD_TO_LP, PostingAtom.REDEEM),
            Currency.HKD);
        put("LOAN_DD_HKD_LP_CASHBACK", "UA_LOAN",
            List.of(PostingAtom.CREDIT_REWARD, PostingAtom.CONVERT_HKD_TO_LP, PostingAtom.CASHBACK),
            Currency.HKD);

        // aliases from sheet wording
        alias("LOAD_DD_HKD", "LOAN_DD_HKD");
        alias("LOAD_DD_LP", "LOAN_DD_LP");
        alias("LOAD_DD_HKD_REDEEM", "LOAN_DD_HKD_REDEEM");
        alias("LOAD_DD_HKD_CASHBACK", "LOAN_DD_HKD_CASHBACK");
        alias("LOAD_DD_LP_REDEEM", "LOAN_DD_LP_REDEEM");
        alias("LOAD_DD_LP_CASHBACK", "LOAN_DD_LP_CASHBACK");
        alias("LOAD_DD_HKD_TO_LP", "LOAN_DD_HKD_TO_LP");
        alias("LOAD_DD_HKD_LP_REDEEM", "LOAN_DD_HKD_LP_REDEEM");
        alias("LOAD_DD_HKD_LP_CASHBACK", "LOAN_DD_HKD_LP_CASHBACK");
        // sheet prose → code
        alias("TRANSACTION_HKD", "CC_TXN_HKD");
        alias("TRANSACTION_LP", "CC_TXN_LP");
    }

    private void put(String code, String profile, List<PostingAtom> atoms, Currency rewardCcy) {
        byCode.put(code, new PostingRecipe(code, profile, atoms, rewardCcy));
    }

    private void alias(String from, String to) {
        PostingRecipe t = byCode.get(to);
        if (t != null) {
            byCode.put(from, new PostingRecipe(from, t.profileHint(), t.atoms(), t.rewardCcy()));
        }
    }

    static String normalize(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT)
            .replace(' ', '_')
            .replace('-', '_')
            .replace(">", "_")
            .replace("__", "_");
    }

    public Optional<PostingRecipe> find(String codeOrEventType) {
        if (codeOrEventType == null || codeOrEventType.isBlank()) {
            return Optional.empty();
        }
        String k = normalize(codeOrEventType);
        return Optional.ofNullable(byCode.get(k));
    }

    public boolean has(String codeOrEventType) {
        return find(codeOrEventType).isPresent();
    }

    public Map<String, PostingRecipe> all() {
        return Map.copyOf(byCode);
    }
}
