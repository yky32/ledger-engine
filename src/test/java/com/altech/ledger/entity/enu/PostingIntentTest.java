package com.altech.ledger.entity.enu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostingIntentTest {

    @Test
    void mapsToOrderTypes() {
        assertEquals(OrderType.DEPOSIT, PostingIntent.DEPOSIT.toOrderType());
        assertEquals(OrderType.WITHDRAWAL, PostingIntent.WITHDRAWAL.toOrderType());
        assertEquals(OrderType.IN_WALLET_TRANSFER, PostingIntent.IN_WALLET_TRANSFER.toOrderType());
        assertEquals(OrderType.EARN, PostingIntent.EARN.toOrderType());
        assertEquals(OrderType.BURN, PostingIntent.BURN.toOrderType());
        assertEquals(OrderType.HOLD, PostingIntent.HOLD.toOrderType());
        assertEquals(OrderType.RELEASE, PostingIntent.RELEASE.toOrderType());
    }
}
