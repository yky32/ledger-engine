package com.altech.ledger.usecase.ledger;

import com.altech.core.constant.enu.Currency;
import com.altech.ledger.entity.enu.PostingAtom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostingRecipeCatalogTest {

    PostingRecipeCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new PostingRecipeCatalog();
    }

    @Test
    void ccTxnLp_singleCredit() {
        var r = catalog.find("CC_TXN_LP").orElseThrow();
        assertEquals(Currency.LP, r.rewardCcy());
        assertEquals(1, r.atoms().size());
        assertEquals(PostingAtom.CREDIT_REWARD, r.atoms().get(0));
        assertEquals("UA_CC", r.profileHint());
    }

    @Test
    void ccTxnHkdToLpRedeem_chain() {
        var r = catalog.find("CC_TXN_HKD_LP_REDEEM").orElseThrow();
        assertEquals(
            java.util.List.of(
                PostingAtom.CREDIT_REWARD,
                PostingAtom.CONVERT_HKD_TO_LP,
                PostingAtom.REDEEM),
            r.atoms());
    }

    @Test
    void loadAlias_sameAtomsAsLoan() {
        assertEquals(
            catalog.find("LOAN_DD_LP").orElseThrow().atoms(),
            catalog.find("LOAD_DD_LP").orElseThrow().atoms());
    }

    @Test
    void purchase_notARecipe() {
        assertTrue(catalog.find("PURCHASE").isEmpty());
    }
}
