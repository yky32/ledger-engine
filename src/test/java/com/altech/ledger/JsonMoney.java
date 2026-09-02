package com.altech.ledger;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/** Read money JSON that may be a number or a currency-scaled string. */
public final class JsonMoney {
    private JsonMoney() {}

    public static BigDecimal bd(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) {
            return null;
        }
        String t = n.asText();
        if (t == null || t.isBlank()) {
            return null;
        }
        return new BigDecimal(t);
    }
}
