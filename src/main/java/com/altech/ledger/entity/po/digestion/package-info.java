/**
 * Persistence for <b>digestion</b> rules (how an inbound event is interpreted and scored).
 * <p>
 * Not the webhook door — that is {@link com.altech.ledger.entity.po.ingest}.
 * A digestion rule answers: which events qualify, currencies/ages, what formula → points.
 *
 * @see com.altech.ledger.entity.po.ingest
 */
package com.altech.ledger.entity.po.digestion;
