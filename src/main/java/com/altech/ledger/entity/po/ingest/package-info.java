/**
 * Persistence for <b>ingest</b> (intake of transactional webhooks).
 * <p>
 * <b>Ingest vs Digestion</b>
 * <ul>
 *   <li>{@code ingest} — door: master switch ({@link com.altech.ledger.entity.po.ingest.IngestPolicy}),
 *       auto-wallet, failed intake store, event payload shapes.</li>
 *   <li>{@code digestion} — brain: match eventType, filters, formula → points/operation
 *       ({@link com.altech.ledger.entity.po.digestion.DigestionRule}).</li>
 * </ul>
 * Pipeline: webhook → IngestPolicy → DigestionRule evaluation → wallet / double-entry earn.
 *
 * @see com.altech.ledger.entity.po.digestion
 */
package com.altech.ledger.entity.po.ingest;
