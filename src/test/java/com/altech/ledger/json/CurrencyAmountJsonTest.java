package com.altech.ledger.json;

import com.altech.core.constant.enu.Currency;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyAmountJsonTest {

    record Row(BigDecimal amount, Currency currency) {}

    record Balances(BigDecimal ledgerBalance, BigDecimal availableBalance, Currency currency) {}

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new MoneyAmountModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void hkdUsesFourPlaces() throws Exception {
        String json = mapper.writeValueAsString(new Row(new BigDecimal("100"), Currency.HKD));
        assertThat(json).contains("\"amount\":\"100.0000\"");
    }

    @Test
    void usdUsesTwoPlaces() throws Exception {
        String json = mapper.writeValueAsString(new Row(new BigDecimal("100"), Currency.USD));
        assertThat(json).contains("\"amount\":\"100.00\"");
    }

    @Test
    void lpUsesZeroPlaces() throws Exception {
        String json = mapper.writeValueAsString(new Row(new BigDecimal("5.9"), Currency.LP));
        assertThat(json).contains("\"amount\":\"5\"");
    }

    @Test
    void defaultTwoPlacesWhenNoCurrency() throws Exception {
        String json = mapper.writeValueAsString(new Row(new BigDecimal("10"), null));
        assertThat(json).contains("\"amount\":\"10.00\"");
    }

    @Test
    void balancesFollowCurrency() throws Exception {
        String json = mapper.writeValueAsString(
            new Balances(new BigDecimal("1"), new BigDecimal("0.5"), Currency.USD));
        assertThat(json).contains("\"ledgerBalance\":\"1.00\"");
        assertThat(json).contains("\"availableBalance\":\"0.50\"");
    }

    @Test
    void readsStringOrNumber() throws Exception {
        Row fromString = mapper.readValue("{\"amount\":\"100.00\",\"currency\":\"USD\"}", Row.class);
        Row fromNumber = mapper.readValue("{\"amount\":100,\"currency\":\"USD\"}", Row.class);
        assertThat(fromString.amount()).isEqualByComparingTo("100.00");
        assertThat(fromNumber.amount()).isEqualByComparingTo("100");
    }
}
