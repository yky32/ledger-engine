# Critical path — upstream event into LedgeRX

Happy path for a credit-card earn (`eventType=CC_TXN`) from the upstream system through Door → Brain → Accounting → Ledger, then optional refund from the movement.

Identity is **1 `ownerId` : 1 wallet**. Books are per `mainAccount` (card) under that wallet. Settlement currency is **HKD**; loyalty is **LP**. Every customer `mainAccount` tree always includes the settlement book (HKD) plus LP.

---

## Journey

```text
Upstream (POS / OMS / card)
    POST /integrations/webhooks/transactions
    {
      eventId, ownerId, eventType, amount, currency,
      occurredAt, mainAccount?, metadata?
    }

1. Door          GET/PUT /ingest-policies
                 eventType + MCC/ccy/amount/age → admit?
                 no wallet + auto-create on:
                   POST /wallets  settlement=HKD
                   open 01-01-01 on event.mainAccount → HKD + LP
                   book names: {ownerId}-HKD , {ownerId}-LP

2. Brain         GET /digestion-rules
                 same eventType, first bingo (priority ASC, id ASC)
                 formula → points + resultCurrency (LP or HKD)

3. Accounting    GET /accounting-rules  (+ executions)
                 bound combo walks CR/DR onto COA (same currency)

                 Txn → LP
                   DR  HOUSE  01-02-01 LP
                   CR  member 01-01-01 LP   ({ownerId}-LP)

                 Txn → HKD
                   DR  HOUSE  01-02-01 HKD
                   CR  member 01-01-01 HKD  ({ownerId}-HKD)

4. Ledger        ledger_movement + 2 ledger_entry legs
                 GET /wallets/{ownerId}
                 GET /wallets/{ownerId}/movements
                 GET /integrations/ledger-entries?movementId=
```

Door, Brain, and Accounting all key off the **same** `eventType` token (`CC_TXN`, `CC_CIP`, `CC_SIP`, `LN_TXN`).

---

## What gets created

| Layer | Persist | Notes |
|---|---|---|
| Wallet | `wallet` | `ownerId`, `settlementCurrency=HKD` |
| Member books | `account` 01-01-01 | HKD + LP on `event.mainAccount`; name `{ownerId}-{ccy}` |
| House books | HOUSE wallet 01-02 operating | Counterparty for earn; skip settlement twin |
| Movement | `ledger_movement` | `orderType=EARN`, `status=SETTLED`, `movementKey=loyalty-earn-{eventId}` |
| Legs | `ledger_entry` × 2 | One DEBIT, one CREDIT, same amount, same currency |

Balances: CREDIT adds, DEBIT subtracts. House operating books allow negative.

---

## Refund — reverse from the movement

Ride the earn. Do **not** re-ingest the webhook.

```http
POST /movements/{id}/refund
```

`{id}` is the **settled EARN** (or BURN) movement.

| Original | Refund |
|---|---|
| `orderType=EARN` | `ADJUSTMENT_REFUND` |
| `amount=10` | `amount=-10` |
| `status=SETTLED` | new row `SETTLED`; original → `REFUNDED` |
| CR member / DR house | **DR member / CR house** (same books, same magnitude) |
| — | `associatedLedgerMovementId` → original |

Idempotent: second `POST` returns the existing refund movement (`movementKey={originalKey}-refund`).

Net of earn + refund: member and house balances back to the pre-earn numbers.

Admin: wallet history → select SETTLED EARN → **Refund · reverse DR/CR**.

---

## APIs (this path only)

| When | Method | Path |
|---|---|---|
| Upstream fire | `POST` | `/integrations/webhooks/transactions` |
| Dry-run | `POST` | `/integrations/webhooks/transactions/dry-run` |
| Door | `GET` `PUT` | `/ingest-policies` |
| Brain | `GET` `POST` `PUT` | `/digestion-rules` |
| Chart | `GET` `POST` | `/coa-profiles` · `/corporate-coa` |
| Accounting | `GET` `POST` | `/accounting-rules` · `/accounting-rule-executions` |
| Wallet | `POST` `GET` | `/wallets` · `/wallets/{ownerId}` |
| History | `GET` | `/wallets/{ownerId}/movements` |
| Legs | `GET` | `/integrations/ledger-entries?movementId=` / `eventId=` |
| Refund | `POST` | `/movements/{id}/refund` |

---

## Skip / fail (not the happy path)

| Code | When |
|---|---|
| `NOT_ENTERED` | Door gates reject (MCC / ccy / amount / age) |
| `NO_WALLET` | No wallet and auto-create off |
| `NO_RULE` / `SKIPPED` | Brain: no first bingo |
| `DUPLICATE` | Same `eventId` already earned (`movementKey` hit) |
| Fail queue | `POST /integrations/failed-transactions/{id}/replay` |

---

See also: [INTEGRATION.md](../INTEGRATION.md) · [BOOKLET.md](./BOOKLET.md) · [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md)
