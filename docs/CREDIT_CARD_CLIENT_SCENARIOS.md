# LedgeRX — Credit card / issuer client scenarios

**Product:** **LedgeRX** · module `ledger-engine`  
**Audience:** product, solutions, client success (credit card / bank / issuer programmes)  
**Status:** Draft for alignment · drives Brain formula + event catalog design  
**Related:** [BRAND.md](./BRAND.md) · [SYSTEM_BUSINESS_FLOW.md](./SYSTEM_BUSINESS_FLOW.md) §5.3–5.5 · [DIGESTION_RULES.md](./DIGESTION_RULES.md) · [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md)

---

## 1. Why this client type matters

A **credit card issuer** (or co-brand bank) is a primary adopter of **LedgeRX** as the **loyalty points system of record**:

| They own | LedgeRX owns |
|----------|----------------|
| Card issuing, credit limit, clearing/settlement with schemes | Wallet + LP books per member |
| Auth / presentment / dispute rails | Earn / burn / hold posting + audit legs |
| MCC, merchant, FX rate at payment time | Configurable digestion of **business events** they send |
| Card product / tier / campaign CRM | Runtime rules (Door + Brain) without redeploy |

**Positioning line:**  
> Upstream (issuer/processor) decides *what happened*; **LedgeRX** decides *how many points book*, and posts **true double-entry** against a programme pool.

---

## 2. Identity & books (always)

```text
Issuer CIF / membership id  →  ownerId  →  1 Wallet
                                              ├── Account HKD (optional settlement / cash mirror)
                                              └── Account LP  (loyalty points)
```

| Rule | |
|------|--|
| Query / webhook key | `ownerId` only |
| Vanity / card last-4 / lucky number | Display only — **never** PK or earn key |
| Multi-currency spend | Event carries spend `currency`; points almost always post to **LP** |

---

## 3. Brain formula model (target: JSON config)

Ops must not rely on cryptic strings. Canonical **formula JSON**:

| type | JSON | Points result | Typical card use |
|------|------|---------------|------------------|
| `AMOUNT` | `{"type":"AMOUNT"}` | = spend amount | 1 LP per 1 unit spend; redeem burn 1:1 |
| `RATE` | `{"type":"RATE","rate":0.01}` | amount × rate | **1% cashback-style** earn |
| `FIXED` | `{"type":"FIXED","value":1000}` | constant | **Card open / birthday** bonus |
| `LINEAR` | `{"type":"LINEAR","rate":0.01,"fixed":50}` | amount×rate + fixed | Promo: 1% + 50 LP boost |

**Legacy strings** (`RATE:0.01`, `FIXED:100`, `MUL_ADD:0.01:5`) may still be accepted on write and normalized to JSON — clients should **only document JSON**.

**Formula does not (today) encode:** MCC, monthly cap, FX conversion, tier ladders, card product id. Those are either:

1. **Upstream-classified `eventType`** + multiple Digestion rules, or  
2. **Future engine features** (see §7 backlog).

---

## 4. Real-life scenario dictionary (issuer)

Each scenario: business meaning · upstream event shape · Door/Brain config · books outcome · fit **today**.

### 4.1 Card open / welcome bonus

| | |
|--|--|
| **Business** | New card approved → one-time LP gift |
| **Upstream** | `eventType=CARD_OPEN` or `SIGNUP`, `amount=0` or `1`, `currency=HKD`, unique `eventId` |
| **Brain** | Rule: eventType `CARD_OPEN`, op `EARN`, formula `{"type":"FIXED","value":1000}` |
| **Books** | CREDIT member LP 1000 · DEBIT PROGRAM LP |
| **Fit** | ✅ Today (`FIXED`, amount can be 0 for non-spend formula) |

---

### 4.2 Domestic retail spend (base earn %)

| | |
|--|--|
| **Business** | Card presentment cleared HKD 200 → 1% = 2 LP |
| **Upstream** | `PURCHASE`, amount `200`, currency `HKD`, `occurredAt`, optional metadata merchantId |
| **Brain** | `{"type":"RATE","rate":0.01}`, minAmount `0.01`, eligibleCurrencies `["HKD"]` |
| **Books** | +2 LP DE vs PROGRAM |
| **Fit** | ✅ Today |

**Example webhook**

```json
{
  "eventId": "auth-or-presentment-uuid",
  "ownerId": "01A12345678",
  "eventType": "PURCHASE",
  "amount": 200,
  "currency": "HKD",
  "occurredAt": "2026-08-13T10:00:00Z",
  "metadata": {
    "channel": "POS",
    "mcc": "5411",
    "merchantName": "PARKnSHOP"
  }
}
```

> **Note:** `mcc` in metadata is **audit only** today — Brain does not branch on MCC unless upstream maps MCC → different `eventType` (e.g. `PURCHASE_GROCERY`).

---

### 4.3 Overseas spend (FX)

| | |
|--|--|
| **Business** | Member spends JPY 10,000; issuer wants earn on **HKD equivalent** or on JPY face |
| **Options** | **A)** Upstream converts to HKD then sends `currency=HKD` amount  
| | **B)** Engine earns on JPY face with `eligibleCurrencies` including JPY + same RATE |
| **Fit** | ⚠️ **Partial** — no in-engine FX table applied inside formula. Prefer **A** for card clients. |

---

### 4.4 Category bonus (e.g. grocery 3%, other 1%)

| | |
|--|--|
| **Business** | MCC 5411 → 3%; else 1% |
| **Today** | DigestionRule `eligibleMccs` e.g. `["5411"]` + webhook `metadata.mcc`. Or still map MCC → dedicated `eventType`. |
| **Fit** | ✅ Native MCC allow-list on Brain rule |

---

### 4.5 Tier / card product (Gold 1.5%, Standard 1%)

| | |
|--|--|
| **Business** | Product code on card drives earn rate |
| **Today** | CRM/issuer sends `eventType=PURCHASE_GOLD` / `PURCHASE_STD` or separate programme wallets (usually **one ownerId one wallet** — product encoded in eventType or metadata + multi-rule) |
| **Fit** | ⚠️ Workarounds · native **tier/product dimension** = backlog |

---

### 4.6 Monthly earn cap (e.g. max 5,000 LP / calendar month)

| | |
|--|--|
| **Business** | Stop earning after monthly ceiling |
| **Today** | **Not in Brain** — upstream must suppress events or send `eventType=PURCHASE_NO_EARN` past cap |
| **Fit** | ❌ Backlog (running total / cap policy) |

---

### 4.7 Tiered rate ladder (first 10k @ 3%, rest @ 1%)

| | |
|--|--|
| **Business** | Progressive earn within period |
| **Today** | Upstream splits amount into two events or pre-computes points and posts fixed earn event |
| **Fit** | ❌ Backlog (`TIER` formula or period accumulator) |

---

### 4.8 Redeem at merchant / statement credit

| | |
|--|--|
| **Business** | Member burns N LP for statement credit or gift |
| **Upstream** | `REDEEM` / `BURN`, amount = LP to burn, currency often `LP` |
| **Brain** | op `BURN`, formula `{"type":"AMOUNT"}` |
| **Books** | DEBIT member LP · CREDIT PROGRAM (if available sufficient) |
| **Fit** | ✅ Today |

---

### 4.9 Auth hold → capture / reverse (reservation)

| | |
|--|--|
| **Business** | Hotel/auth holds points; capture burns or release on cancel |
| **Engine** | `HOLD` reduces **available** only; `RELEASE` restores; ledger total unchanged until true BURN |
| **Fit** | ✅ Today — [HOLD_RELEASE.md](./HOLD_RELEASE.md) |

```text
Auth approved → HOLD 500 LP (available ↓)
Checkout OK   → RELEASE 500 + REDEEM/BURN 500
                or single BURN if product allows
Cancel        → RELEASE 500
```

---

### 4.10 Annual fee / paid upgrade bonus

| | |
|--|--|
| **Business** | Fee charged on card → gift LP |
| **Upstream** | `ANNUAL_FEE` or `FEE_PAID`, amount = fee in HKD |
| **Brain** | `FIXED` bonus **or** `RATE` on fee amount |
| **Fit** | ✅ with dedicated eventType |

---

### 4.11 Refund / chargeback clawback

| | |
|--|--|
| **Business** | Original purchase reversed → claw back earned LP |
| **Upstream** | Prefer **linked** `eventId` / metadata `originalEventId`; send `REFUND` or `CLAWBACK` with amount = original spend or points |
| **Brain** | `BURN` with `AMOUNT` or `RATE` matching original earn policy |
| **Books** | Reverse economic effect (member LP ↓) |
| **Fit** | ⚠️ **Partial** — no automatic link to original legs; issuer must compute clawback points and send explicit burn event. Backlog: reverse-by-original-eventId |

---

### 4.12 Statement cycle / expiry

| | |
|--|--|
| **Business** | Points expire after 24 months |
| **Fit** | ❌ Out of scope today (no expiry cron product). Track as programme backlog / external job calling BURN |

---

### 4.13 Partner / co-brand campaign burst

| | |
|--|--|
| **Business** | Weekend 5x points at partner |
| **Today** | Time-box via upstream only sending `PURCHASE_PARTNER` during window **or** ops enables high-priority rule then disables |
| **Fit** | ⚠️ Ops process · no calendar engine in Brain |

---

### 4.14 Failed ingest & ops recovery

| | |
|--|--|
| **Business** | JPY not in eligible list → member complains missing points |
| **Engine** | `SKIPPED` + fail queue row → ops adds JPY → **replay** |
| **Fit** | ✅ Today |

---

## 5. Expanded §5.3 example set (for business review)

### 5.3-A Base retail earn

| Step | Detail |
|------|--------|
| Config | Door on + auto-wallet HKD+LP; Brain `PURCHASE` RATE 0.01 |
| Event | HKD 200 PURCHASE |
| Result | **EARNED** · +2 LP · 2 DE legs |
| Audit | `GET /integrations/ledger-entries?eventId=` |

### 5.3-B Welcome + first spend

| Step | Detail |
|------|--------|
| Events | (1) `CARD_OPEN` FIXED 1000 (2) `PURCHASE` 500 HKD @ 1% |
| Result | +1000 then +5 LP |
| Note | Two eventIds; order can be either if auto-wallet on |

### 5.3-C Grocery vs general (issuer-mapped types)

| Step | Detail |
|------|--------|
| Config | `PURCHASE_GROCERY` RATE 0.03 priority 10; `PURCHASE` RATE 0.01 priority 100 |
| Event | Upstream MCC 5411 → `PURCHASE_GROCERY` amount 300 |
| Result | +9 LP |

### 5.3-D Overseas (issuer FX first)

| Step | Detail |
|------|--------|
| Upstream | JPY 10,000 → HKD 520 equivalent |
| Event | PURCHASE amount 520 currency HKD |
| Result | RATE 0.01 → +5.2 LP |

### 5.3-E Redeem + hold path

| Step | Detail |
|------|--------|
| HOLD 100 LP | available −100 |
| REDEEM/BURN 100 | ledger −100 (after release or direct burn per product rules) |

### 5.3-F Duplicate presentment

| Step | Detail |
|------|--------|
| Same `eventId` twice | Second → **DUPLICATE** · no double earn |

---

## 6. Recommended eventType catalog (issuer starter pack)

| eventType | op | formula (JSON) | Notes |
|-----------|-----|----------------|-------|
| `CARD_OPEN` | EARN | FIXED 1000 | Welcome |
| `PURCHASE` | EARN | RATE 0.01 | Default domestic |
| `PURCHASE_GROCERY` | EARN | RATE 0.03 | Optional category |
| `PURCHASE_ONLINE` | EARN | RATE 0.015 | Optional channel |
| `PURCHASE_FX` | EARN | RATE 0.01 | If earn on FX face without convert |
| `REDEEM` | BURN | AMOUNT | Statement / gift |
| `REFUND` | BURN | RATE 0.01 or AMOUNT | Clawback — align with earn |
| `ANNUAL_FEE` | EARN | FIXED or RATE | Fee thank-you |
| `ADJUSTMENT` | EARN/BURN | FIXED | Ops goodwill / correction |

Door: `isEnabled=true`, `isAutoCreateWallet=true` (or false if cards always onboard at issuance).

---

## 7. Requirements backlog (engine)

Prioritised for **credit card / finance** adoption:

| ID | Capability | Client pain | Suggested design |
|----|------------|-------------|------------------|
| CC-1 | **JSON formula only** (done/in progress) | String DSL hard to explain | `formula` JSONB `{type,rate,value,fixed}` |
| CC-2 | **MCC / category match** | Grocery vs general | ✅ **Done** — `eligibleMccs` + metadata.mcc |
| CC-3 | **Period earn cap** | Monthly max LP | Policy + running counter per ownerId |
| CC-4 | **Tier / product rate** | Gold vs standard | `owner attributes` or event dimension table |
| CC-5 | **Ladder rates** | First N @ higher % | Formula type `TIER` + period base |
| CC-6 | **FX-at-earn** | JPY spend → HKD LP base | Hook FX rate service before RATE |
| CC-7 | **Clawback by original event** | Chargeback | `reversesEventId` → mirror legs |
| CC-8 | **Expiry / breakage** | Points aging | Batch BURN job + policy |
| CC-9 | **Campaign calendar** | Weekend 5x | Rule `validFrom`/`validTo` |
| CC-10 | **Card product multi-wallet** | Multi-brand one CIF | Revisit 1:1 wallet rule **only if** product requires |

**Near-term go-live path (no CC-2…):** issuer middleware maps MCC/tier/FX → `eventType` + amount already in earn currency; engine stays Door + Brain JSON + Books.

---

## 8. Fit summary

| Area | Today | With eventType mapping | Needs build |
|------|-------|------------------------|-------------|
| % earn on spend | ✅ RATE | ✅ | |
| Fixed bonuses | ✅ FIXED | ✅ | |
| Redeem / burn | ✅ | ✅ | |
| Hold / release | ✅ | ✅ | |
| Fail queue / replay | ✅ | ✅ | |
| DE audit legs | ✅ | ✅ | |
| Category / MCC native | ✅ `eligibleMccs` + `metadata.mcc` | ✅ | |
| Monthly cap | ❌ | ⚠️ upstream | CC-3 |
| Tier native | ❌ | ⚠️ via types | CC-4 |
| Ladder | ❌ | ⚠️ upstream split | CC-5 |
| In-engine FX | ❌ | ⚠️ upstream FX | CC-6 |
| Auto clawback link | ❌ | ⚠️ manual burn | CC-7 |
| Expiry | ❌ | ❌ | CC-8 |

---

## 9. Client conversation checklist

1. Who is `ownerId` (CIF / cardholder id / membership)?  
2. Earn base currency after FX — who converts?  
3. Category/tier — middleware eventTypes or engine filters?  
4. Caps / ladders — upstream or engine roadmap?  
5. Hold at auth — required?  
6. Chargeback clawback process?  
7. Auto-wallet at first presentment vs onboard at card issuance?  
8. Reporting — legs by eventId enough for finance audit?

---

## 10. Doc history

| Date | Note |
|------|------|
| 2026-08-13 | Initial draft from product discussion (issuer fit + scenario dictionary) |

**Next:** product rank CC-2…CC-7 · keep formula JSON as ops-facing contract · optional admin UI formula builder bound to this catalog.
