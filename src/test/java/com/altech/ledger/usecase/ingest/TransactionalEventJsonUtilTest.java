package com.altech.ledger.usecase.ingest;

import com.altech.core.utils.JSONUtil;
import com.altech.ledger.entity.dto.ingest.TransactionalEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalEventJsonUtilTest {

    @Test
    void restJson() {
        String json = """
            {
              "eventId": "evt-1",
              "ownerId": "01A31658334",
              "eventType": "CC_TXN",
              "amount": 100,
              "currency": "HKD",
              "occurredAt": "2026-09-02T06:40:00Z",
              "mainAccount": "908951901284",
              "metadata": { "mcc": "5411", "channel": "UAF_CC" }
            }
            """;
        TransactionalEvent e = JSONUtil.readValue(json, TransactionalEvent.class);
        assertThat(e.eventId()).isEqualTo("evt-1");
        assertThat(e.ownerId()).isEqualTo("01A31658334");
        assertThat(e.eventType()).isEqualTo("CC_TXN");
        assertThat(e.amount()).isEqualByComparingTo("100");
        assertThat(e.currency().getIsoCode()).isEqualTo("HKD");
        assertThat(e.mainAccount()).isEqualTo("908951901284");
        assertThat(e.metadata()).containsEntry("mcc", "5411");
    }

    @Test
    void sdkAliasesAndAmountString() {
        String json = """
            {
              "eventId": "evt-2",
              "userId": "01A31658334",
              "eventType": "CC_TXN",
              "amount": "10.00",
              "currency": "HKD",
              "metadata": { "mcc": 5411 }
            }
            """;
        TransactionalEvent e = JSONUtil.readValue(json, TransactionalEvent.class);
        assertThat(e.ownerId()).isEqualTo("01A31658334");
        assertThat(e.amount()).isEqualByComparingTo("10.00");
        assertThat(e.metadata()).containsEntry("mcc", "5411");
    }

    @Test
    void envelopePayload() {
        String json = """
            {
              "requestId": "Tabc",
              "eventName": "CC_TXN",
              "payload": {
                "eventId": "evt-3",
                "ownerId": "01A31658334",
                "eventType": "CC_TXN",
                "amount": 50,
                "currency": "HKD"
              }
            }
            """;
        TransactionalEvent e = JSONUtil.readValue(json, TransactionalEvent.class);
        assertThat(e.eventId()).isEqualTo("evt-3");
        assertThat(e.amount()).isEqualByComparingTo("50");
    }

}
