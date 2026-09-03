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

    @Test
    void refundSplitsActionAndOriginalEventId() {
        String json = """
            {
              "eventId": "evt-1-refund",
              "ownerId": "01A31658334",
              "eventType": "CC_TXN",
              "action": "REFUND",
              "originalEventId": "evt-1",
              "amount": "100.00",
              "currency": "HKD"
            }
            """;
        TransactionalEvent e = JSONUtil.readValue(json, TransactionalEvent.class);
        assertThat(e.isRefund()).isTrue();
        assertThat(e.eventType()).isEqualTo("CC_TXN");
        assertThat(e.action().name()).isEqualTo("REFUND");
        assertThat(e.originalEventId()).isEqualTo("evt-1");
    }

    @Test
    void voidAndPartialParse() {
        TransactionalEvent v = JSONUtil.readValue("""
            {"eventId":"v1","ownerId":"o","eventType":"CC_TXN","action":"VOID",
             "originalEventId":"evt-1","amount":"0","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(v.action().isFullReverse()).isTrue();
        assertThat(v.action().name()).isEqualTo("VOID");

        TransactionalEvent p = JSONUtil.readValue("""
            {"eventId":"p1","ownerId":"o","eventType":"CC_TXN","action":"PARTIAL",
             "originalEventId":"evt-1","amount":"40.00","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(p.action().isUnsupported()).isTrue();
        assertThat(p.action().isFullReverse()).isFalse();
    }

    @Test
    void chargebackAndAdjustParse() {
        TransactionalEvent c = JSONUtil.readValue("""
            {"eventId":"c1","ownerId":"o","eventType":"CC_TXN","action":"CHARGEBACK",
             "originalEventId":"evt-1","amount":"100.00","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(c.action().isFullReverse()).isTrue();
        assertThat(c.action().name()).isEqualTo("CHARGEBACK");

        TransactionalEvent a = JSONUtil.readValue("""
            {"eventId":"a1","ownerId":"o","eventType":"CC_TXN","action":"ADJUST",
             "originalEventId":"evt-1","amount":"5.00","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(a.action().isUnsupported()).isTrue();
        assertThat(a.action().name()).isEqualTo("ADJUST");
    }

    @Test
    void actionAliasesAndSpendDefault() {
        TransactionalEvent spend = JSONUtil.readValue("""
            {"eventId":"s1","ownerId":"o","eventType":"CC_TXN","amount":"10","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(spend.action().name()).isEqualTo("SPEND");
        assertThat(spend.isRefund()).isFalse();

        TransactionalEvent original = JSONUtil.readValue("""
            {"eventId":"s2","ownerId":"o","eventType":"CC_TXN","action":"ORIGINAL",
             "amount":"10","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(original.action().name()).isEqualTo("SPEND");

        TransactionalEvent reverse = JSONUtil.readValue("""
            {"eventId":"v2","ownerId":"o","eventType":"CC_TXN","action":"REVERSE",
             "originalEventId":"evt-1","amount":"0","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(reverse.action().name()).isEqualTo("VOID");

        TransactionalEvent dispute = JSONUtil.readValue("""
            {"eventId":"c2","ownerId":"o","eventType":"CC_TXN","action":"DISPUTE",
             "originalEventId":"evt-1","amount":"100","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(dispute.action().name()).isEqualTo("CHARGEBACK");
    }

    @Test
    void legacyEventTypeSuffixInfersAction() {
        TransactionalEvent refund = JSONUtil.readValue("""
            {"eventId":"r1","ownerId":"o","eventType":"CC_TXN_REFUND",
             "originalEventId":"evt-1","amount":"100","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(refund.action().name()).isEqualTo("REFUND");
        assertThat(refund.isRefund()).isTrue();

        TransactionalEvent voided = JSONUtil.readValue("""
            {"eventId":"v3","ownerId":"o","eventType":"CC_TXN_VOID",
             "originalEventId":"evt-1","amount":"0","currency":"HKD"}
            """, TransactionalEvent.class);
        assertThat(voided.action().name()).isEqualTo("VOID");
    }

}
