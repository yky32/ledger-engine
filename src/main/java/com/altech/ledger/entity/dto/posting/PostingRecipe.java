package com.altech.ledger.entity.dto.posting;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.PostingAtom;

import java.util.List;

/**
 * One product use-case → ordered atoms. Outer code only; COA is internal.
 *
 * @param code        stable key = webhook / Brain {@code eventType} (e.g. CC_TXN)
 * @param profileHint UA_CC | UA_LOAN (documentation / future coaProfile filter)
 * @param atoms       execution order
 * @param rewardCcy   if non-null, force reward book currency for CREDIT_REWARD
 */
public record PostingRecipe(
    String code,
    String profileHint,
    List<PostingAtom> atoms,
    Currency rewardCcy
) {
    public PostingRecipe {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("recipe code required");
        }
        code = code.trim().toUpperCase();
        if (atoms == null || atoms.isEmpty()) {
            throw new IllegalArgumentException("recipe atoms required: " + code);
        }
        atoms = List.copyOf(atoms);
    }
}
