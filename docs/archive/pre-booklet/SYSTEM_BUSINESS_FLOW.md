# LedgeRX — System Business Flow

> **Docs entry:** [START_HERE.md](./START_HERE.md) — 唔好從本檔開始亂跳。

**Audience:** product / business review (e.g. @wilfredkan)  
**Status:** Draft for alignment · reflects **shipped** engine behaviour on main  
**Product:** **LedgeRX** (`ledger-engine`) — wallet + loyalty ledger core (not payment rails, not CRM)  
**Brand:** [BRAND.md](./BRAND.md)

---

## 1. What this system is

**LedgeRX** is the **system of record for balances and loyalty points** for a customer programme.

| It does | It does **not** |
|---------|------------------|
| Own wallets & multi-currency books | Own customer master / CRM |
| Receive **business events** from upstream (POS, e‑com, campaign) | Process card payments / PSP |
| Apply **configurable** earn/burn rules at runtime (currency / MCC / age + formula) | Host marketing campaign UI |
| Post **double-entry** loyalty legs (audit-grade) | Multi-tenant SaaS billing |
| Hold / release spendable points | Tier ranking / complex promotions engine |

**Identity model:** one external customer id (`ownerId`, e.g. CRM `01A########`) → **exactly one Wallet**.

```text
CRM Customer id
      │
      ▼
   Wallet  (settlement currency, e.g. HKD)
      ├── Account HKD   (optional cash book)
      └── Account LP    (loyalty points book)
```

---

## 2. Actors

| Actor | Role |
|-------|------|
| **Upstream system** | POS / order / campaign — sends transactional events |
| **CRM / membership** | Owns customer id; may batch-create wallets |
| **Ops / programme admin** | Configures ingest policy + digestion rules (no deploy) |
| **Ledger Engine** | Applies rules, posts balances, stores failures, exposes query |
| **Downstream (future)** | App / portal reads balances & history (out of current UI scope) |

---

## 3. End-to-end business picture

```text
                    ┌──────────────────────┐
                    │  Ops configures once │
                    │  · Ingest policy     │
                    │  · Digestion rules   │
                    └──────────┬───────────┘
                               │ runtime DB (no restart)
                               ▼
┌─────────────┐    webhook     ┌──────────────────────────────────────────┐
│  Upstream   │ ─────────────▶ │           LEDGER ENGINE                  │
│  POS / OMS  │   event JSON   │                                          │
└─────────────┘                │  1) Door  — accept event? auto-wallet?   │
                               │  2) Brain — match rule + score points    │
                               │  3) Books — double-entry on LP (+ HKD)   │
                               │  4) Audit — legs + movement history      │
                               └──────────────────────────────────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    ▼                    ▼                    ▼
              Customer LP            PROGRAM pool         Fail queue
              balance ↑/↓            counterparty         (ops replay)
```

---

## 4. Two configuration concepts (important)

These must stay separate in product language:

| | **Ingest policy** (door) | **Digestion rules** (brain) |
|--|--------------------------|-----------------------------|
| Business question | “Do we accept webhooks at all? Create wallet if missing?” | “Which events earn/burn, and how many points?” |
| Cardinality | ~1 global policy | Many rules (by event type / priority) |
| Change | Ops API — effective immediately | Ops API — effective immediately |
| Example | Kill-switch off for incident | PURCHASE → 1% LP; SIGNUP → 100 LP |

```text
Event in
   → Ingest policy     (door)
   → Digestion rule(s) (brain)
   → Wallet + ledger
```

---

## 5. Core business flows

### 5.1 Programme setup (ops)

1. Deploy engine + DB.  
2. Bootstrap defaults (or configure manually):
   - Ingest: enabled + auto-create wallet (HKD settlement + LP book).  
   - Digestion: e.g. PURCHASE earn 1%, SIGNUP fixed 100 LP, REDEEM burn.  
3. Rules/policy can be changed later **without release**.

**Business outcome:** programme is live for event intake.

---

### 5.2 Customer wallet (membership)

**Path A — Explicit onboard (CRM)**  
CRM creates wallet when member joins: settlement currency + LP book.

**Path B — Lazy onboard (default for adopt)**  
First **eligible** upstream event creates wallet automatically (same transaction as earn), if policy allows.

**Business outcome:** customer can hold LP (and optional HKD book).

---

### 5.3 Earn points (primary loyalty flow)

**Trigger:** upstream completes a business transaction (e.g. purchase).

```text
Upstream: "Customer 01A… bought HKD 200 at store"
                │
                ▼
         Webhook event
         · eventId (idempotency)
         · customer id
         · eventType = PURCHASE
         · amount + currency + time
                │
                ▼
         Digestion matches PURCHASE rule
         · eligibility: min amount, currency list, **MCC list**, max age
         · formula e.g. points = amount × 1%
                │
                ▼
         Double-entry (balanced)
         · DEBIT  programme pool LP
         · CREDIT customer LP
                │
                ▼
         Customer sees +points; audit has two legs
```

| Result | Meaning |
|--------|---------|
| **EARNED** | Points posted; legs available |
| **DUPLICATE** | Same `eventId` already processed (safe retry) |
| **SKIPPED** | Failed a gate / no rule / disabled — stored for ops |

**Example:** HKD 200 purchase @ 1% → **2 LP** credited.

**More issuer / credit-card scenarios** (welcome bonus, overseas FX, category, hold, clawback, caps):  
→ [CREDIT_CARD_CLIENT_SCENARIOS.md](./CREDIT_CARD_CLIENT_SCENARIOS.md) §4–5.

---

### 5.4 Burn points (redeem)

**Trigger:** upstream redemption / spend of points.

```text
Upstream: "Customer redeems N LP"
   → Digestion REDEEM rule (BURN)
   → DEBIT customer LP · CREDIT programme pool
   → Balance reduced if available sufficient
```

---

### 5.5 Hold / release (reservation)

**Business need:** reserve points for an open order without “spending” them in the ledger sense.

| Action | Available (spendable) | Ledger (owned) |
|--------|------------------------|----------------|
| **HOLD** | ↓ | unchanged |
| **RELEASE** | ↑ (≤ ledger) | unchanged |

**Example:** cart holds 3 LP → available drops 3; order cancel → release restores available.

---

### 5.6 Failure handling & replay (ops)

When an event is skipped (wrong currency, too old, no rule, etc.):

1. Engine stores a **failed ingest** row (payload + reason).  
2. Returns **SKIPPED** to upstream (upstream may retry or ignore).  
3. Ops can **review** or **replay** after fixing config (e.g. add JPY to eligible currencies).  
4. Replay does not spam duplicate fail rows if still invalid.

**Business outcome:** controllable recovery without silent data loss.

---

### 5.7 Inquiry & audit

| Need | Capability |
|------|------------|
| Current balances | Query wallet / accounts (ledger + available) |
| Movement history | Filter by type, currency, date range |
| Point-in-time | Balance **as-of** a timestamp |
| Earn proof | Query **debit + credit legs** by event or movement |

---

## 6. Double-entry (why finance cares)

Every earn/burn is **two-sided**:

```text
EARN  N LP     BURN  N LP
  DR Programme pool    DR Customer
  CR Customer          CR Programme pool
```

- Customer liability / points outstanding stays reconcilable.  
- Programme pool is a system book (`PROGRAM`), not a human customer.  
- Legs are returned on earn and queryable later.

---

## 7. Scoring formulas (business view)

Configured per digestion rule (changeable live):

| Formula style | Business meaning | Example |
|---------------|------------------|---------|
| Rate | % of spend | 1% of HKD amount → LP |
| Fixed | Flat grant | Signup → 100 LP |
| Amount | 1:1 of amount field | Redeem face value |
| Rate + fixed | Hybrid | 1% + 5 bonus LP |

Not in scope today: full expression language, tiers, multi-campaign stack UI.

---

## 8. Sequence — happy path earn (swimlane)

```text
Upstream          Ledger Engine              Books
   │                    │                      │
   │  POST event        │                      │
   │───────────────────▶│                      │
   │                    │ policy OK?           │
   │                    │ rule match + score   │
   │                    │ ensure wallet        │
   │                    │─────────────────────▶│ post DE legs
   │                    │◀─────────────────────│
   │  EARNED + legs     │                      │
   │◀───────────────────│                      │
```

---

## 9. Out of scope (explicit)

| Topic | Notes |
|-------|--------|
| Payment capture / refund rails | Upstream or PSP |
| CRM profile / KYC UI | CRM owns |
| Campaign builder UI | Configure via API/ops for now |
| Tier / VIP engine | Client-side or later epic |
| AuthN/Z product | Planned; local open for pre-UAT |
| Multi-wallet per customer | Product rule is 1:1 |

---

## 10. Open questions for review (@wilfredkan)

Please flag anything that should change before wider adopt:

1. **Lazy wallet create** on first earn — acceptable default, or always CRM-first?  
2. **Earn base currency list** (default HKD/USD only) — enough for v1 markets?  
3. **Hold** semantics — enough for cart/reservation, or need expiry timer next?  
4. **Burn** source of truth — always upstream event, or also in-app API later?  
5. **Reporting** — is leg query + as-of enough for finance v1, or need export/GL file?

---

## 11. Related technical docs (implementers)

| Doc | Content |
|-----|---------|
| [README.md](./README.md) | Docs index |
| [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) | Upstream webhook playbook |
| [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md) | Door vs brain |
| [DOUBLE_ENTRY_EARN.md](./DOUBLE_ENTRY_EARN.md) | PROGRAM pool legs |
| [HOLD_RELEASE.md](./HOLD_RELEASE.md) | Hold / release |
| [INTEGRATION.md](../INTEGRATION.md) | Integration overview |
| [PRODUCT.md](../PRODUCT.md) | Broader product catalogue |

---

*Draft for business review · Ledger Engine · WY / eng*  
*Update this page when product decisions close the open questions.*
