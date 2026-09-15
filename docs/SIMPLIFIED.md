# LedgeRX — Simplified

Condensed view of [BOOKLET.md](BOOKLET.md): what the system must do (functional requirements), how an event flows, the tech stack, and the business rules that govern balances.

---

## 1. What it is

**LedgeRX** is the *points / multi-currency wallet system of record* for loyalty programs.

> Earn · burn · books — without redeploy.

One sentence: **upstream systems tell it what happened (a card spend, a loan payment, a Facebook like); rules decide how many points that earns; double-entry books record the balance.**

| Component | Repo | Role |
|---|---|---|
| Engine | `ledger-engine` | REST API + rules + posting (this repo) |
| Admin portal | `ledger-engine-admin-portal` | Ops UI to manage rules, wallets, refunds |
| SDK | `ledger-engine-sdk` | Optional Java client JAR (versioned, not on Maven Central) |

**In scope:** ledger core (wallets, balances, rules, postings, holds, refunds).
**Out of scope:** payment gateways / card rails, CRM / identity, compliance UI, settlement orchestration.

---

## 2. Tech stack

| Layer | Choice |
|---|---|
| Language / framework | Java 17 · Spring Boot 3.5.x |
| Database | PostgreSQL (local `:5433`, DB `ledger-engine`) |
| Schema | `JPA_DDL_AUTO=create` pre-UAT → Flyway later (TD-OPS-001) |
| Async ingest | Kafka (off by default; `LEDGER_KAFKA_ENABLED=true`, topic `ledger.transaction.events`) |
| API envelope | `R.success` / `Result` (`com.altech.core`); errors = `BizException` + `*ErrorResponse` |
| JSON conventions | camelCase; money amounts as currency-scaled **strings** (e.g. `"100.00"`) |
| Deploy | In-cluster / client VPC; Docker Compose locally |
| Tooling | Admin portal, generic CRM simulator (`simulator/`), bootstrap & smoke scripts |

---

## 3. Functional requirements

### FR-1 · Wallets & accounts
- **1 `ownerId` → exactly 1 wallet** (hard product rule; `ownerId` = CRM customer id).
- Each wallet holds **multiple currency accounts**: settlement HKD + loyalty LP at minimum.
- Account identity = `fullNumber`: `entity(2)+type(2)+subType(2)+mainAccount+buffer(2)+currency(3)`, e.g. `01 01 01 908951901284 00 344` → member HKD book.
- Balances live on the account: `ledgerBalance` + `availableBalance`, updated **under lock**.
- Display name `{ownerId}-{iso}` is computed on GET, never stored. Customer names live in CRM — the ledger stores only `ownerId`.
- Onboarding: `POST /wallets` (requires `ownerId` + `settlementCurrency`), batch up to 1000, duplicate → `WAL0409`. Optional **auto-wallet**: first eligible event creates HKD+LP in the same transaction.

### FR-2 · Event ingest (earn engine)
- Accept **transactional events** via webhook REST or Kafka — identical JSON either way (`IngestTransactionUseCase`).
- Alias tolerance: `ownerId` ← `associatedIdentifier` / `userId`; unwraps `payload`/`data`/`event`/`body`; stringifies metadata values.
- Support product families by `eventType`: `CC_TXN` (credit card), `CC_CIP`, `CC_SIP`, `LN_TXN` (loan), plus non-financial types (e.g. `LIKE_FB_PAGE`).
- **Idempotency**: `movementKey = loyalty-earn-{eventId}` / `loyalty-burn-{eventId}` — same `eventId` twice → `DUPLICATE`, no double credit.

### FR-3 · Configurable scoring (no redeploy)
- Door (admission) and Brain (scoring) are **data**: ops edit factors/rules via API/Admin, engine picks them up with no deploy.
- Formula types: `RATE`, `FIXED`, `LINEAR`, `AMOUNT`, `TIERED_RATE`, `TABLE`; optional `multiplier` / `cap` / `floor`. JSON only — no SpEL.
- Reward can be loyalty points (LP) or cashback (HKD), set by the rule's `resultCurrency`.

### FR-4 · Money movement & holds
- Deposits, withdrawals, in-wallet transfers (`/movements/...`) — all through the posting pipeline.
- **Holds**: `POST /wallets/holds` reduces *available* only (ledger untouched); `POST /wallets/releases` restores it (never above ledger).

### FR-5 · Refunds / voids / chargebacks
- Two entry points, one mechanism: ops click `POST /movements/{id}/refund`, or an upstream event with `action=REFUND|VOID|CHARGEBACK` + `originalEventId`.
- Full reverse of the original legs (DR/CR swapped), Brain **not** re-scored; original → `REFUNDED`, reverse row → `SETTLED`.
- Reverse is idempotent across all three actions (`movementKey={originalKey}-refund`).
- `PARTIAL` and `ADJUST` are recognised but not booked yet → `ACTION_UNSUPPORTED`.

### FR-6 · Wallet tiering
- Policies via `GET/PUT /wallet-tier-policies`; criterion v1 = `LEDGER_BALANCE` (sum of ledger balances for wallet+currency, default LP).
- After each settled movement the engine re-assesses the tier **in the same transaction**: upgrade when `ledgerBalance >= upgradeAt`, downgrade when `< downgradeBelow`. Refunds can downgrade; holds can't move tier; HOUSE wallet skipped.

### FR-7 · Ops surface
- Admin portal screens: Door, Brain (formula editor), Webhook dry-run (with match trace), Wallets, Chart, Rules, Ingest, Ledger, Refund.
- Failed events land in a fail queue → `POST /integrations/failed-transactions/{id}/replay`.
- Full audit of legs: `/integrations/ledger-entries?movementId=`, history `/wallets/{ownerId}/movements`, as-of queries.
- SDK handshake: `GET /integrations/sdk-info`; dry-run endpoint for testing rules without booking.

---

## 4. The flow (one pipeline)

```text
TransactionalEvent  (REST webhook  OR  Kafka — same JSON)
      │
      ▼
┌──────────────┐  DOOR — "admit?"
│ ingest-      │  entryFactors: isEnabled + field matchers (MCC, currency, amount, age…)
│ policies     │  auto-wallet: create HKD+LP here if enabled
└──────┬───────┘
       │ entered
       ▼
┌──────────────┐  BRAIN — "how many points?"
│ digestion-   │  whenFactors (+ legacy minAmount/ccy filters, ANDed)
│ rules        │  formula → points + resultCurrency
└──────┬───────┘  first matching rule wins (priority ASC, id ASC) — no stacking
       │
       ▼
┌──────────────┐  ACCOUNTING — "which books?"
│ coa profile  │  eventType → COA profile → segments + currency
│ + rules      │  walk CR/DR legs, same currency on both sides
└──────┬───────┘
       │
       ▼
ApplyPostingUseCase → ledger_movement + 2 ledger_entry legs → balances (locked)
```

All three stages key off the **same `eventType` token**. Two extra optional event fields:
- `action` — how to book *this* fire: omit = `SPEND`; `REFUND`/`VOID`/`CHARGEBACK` reverse an original; `PARTIAL`/`ADJUST` not yet supported.
- `applyTo` — which balance to write: default **BOTH**; `LEDGER` = AUTH/pending only; `AVAILABLE` = post-AUTH spend (capped at ledger). Reversals copy the original's `applyTo`.

### Critical path — CC_TXN earn, then refund

```text
1. Upstream fires  { eventId, ownerId, eventType:CC_TXN, amount:"100.00", currency:HKD, mcc:5411 }
2. Door admits     (gate factors pass; wallet auto-created if needed)
3. Brain scores    rule RATE 1% → 5 LP   (500 HKD × 1%)
4. Accounting      DR HOUSE 01-02-01 LP  /  CR member 01-01-01 LP   ← EARN
5. Movement        orderType=EARN, status=SETTLED, key=loyalty-earn-{eventId}

Refund later (action=REFUND + originalEventId, or ops click):
   DR member / CR house, amount −10 → new SETTLED row, original → REFUNDED
   Brain NOT re-scored · idempotent · net balances = pre-earn
```

**Outcome codes (non-happy path):** `NOT_ENTERED` (door gate) · `NO_WALLET` (auto-create off) · `NO_RULE`/`SKIPPED` (no brain match) · `DUPLICATE` (replayed eventId) · `ACTION_UNSUPPORTED` (PARTIAL/ADJUST) · `NO_ORIGINAL` (reverse without originalEventId).

---

## 5. Business logic — the rules that matter

### Double-entry model
- Every movement = **2+ legs, DR = CR, same currency** on both sides. Legs stored as positive amounts + `MovementDirection`.
- Loyalty convention: **CREDIT = add to member**, DEBIT = subtract. T-accounts: DR left / CR right.
- House books (`ownerId=HOUSE`, operating `01-02-01`) are the earn/burn counterparty and may go negative.

| Movement | Member book | Counterparty |
|---|---|---|
| EARN | + | HOUSE operating (DE) |
| BURN | − | HOUSE operating (DE) |
| DEPOSIT / WITHDRAWAL | ± | single-sided |
| IN_WALLET_TRANSFER | ± | other wallet |
| HOLD / RELEASE | available only | ledger unchanged |
| ADJUSTMENT_REFUND | reversed legs | same books |

**Balance math by `applyTo`:**

| Action | ledger | available |
|---|---|---|
| EARN (default BOTH) | ↑ | ↑ |
| EARN `LEDGER` | ↑ | — |
| EARN `AVAILABLE` | — | ↑ (≤ ledger) |
| HOLD | — | ↓ |
| RELEASE | — | ↑ (≤ ledger) |
| BURN | ↓ | ↓ |

### Single write path (invariant)
**All** balance writes go through `ApplyPostingUseCase` (via `PostingCommand`/`PostingIntent`). Earning via the deposit API or bypassing the use case is forbidden — it breaks the HOUSE double-entry and causes drift.

```java
applyPostingUseCase.execute(PostingCommand.earn(...));   // never deposit() for an earn
```

### Rule matching
- One shared factor matcher (Door `entryFactors`, Brain `whenFactors`): fields `currency · mcc · amount · ageDays · eventType · metadata.*`, ops `eq/in/gte/between/startsWith/…`, combinators `all/any/atLeast/exactly/not/oneOf/anyGroup`.
- Every decision is explainable: `eligibilityTrace[].matchedPath` (e.g. `G12 > F1 > currency:eq`) — surfaced in webhook response and Admin dry-run.
- Brain picks the **first** rule that matches (priority ASC, id ASC). No rule stacking (parked).
- Legacy columns (`minAmount`, currency, mcc, age) still AND with the JSON factors.

### COA (chart of accounts) — internal, not a public API concern
- Product events carry **only** `eventType`; the engine maps it → COA profile → segments + points currency. No COA segments per transaction.
- Key books: member custodian `01-01-01`, house operating `01-02-01` (mainAccount 9999), house expense `01-04-02`.
- Default: profile `code` ≡ `transactionCode` ≡ webhook `eventType` (e.g. `CC_TXN`).

### Other logic
- **Non-financial engagement** (e.g. `LIKE_FB_PAGE`): same pipeline, `amount=0` allowed, `FIXED` formula → flat LP.
- **Recipes** (UA sheet use cases): `eventType|metadata.useCase` → `PostingRecipeCatalog` → atoms (`CREDIT_REWARD`, `REDEEM`, `CASHBACK`, `CONVERT_HKD_TO_LP`) → posting. CC recipes compose atoms, e.g. `CC_TXN_HKD_TO_LP` = earn HKD + convert 1:1 to LP.
- **Outbound Kafka**: after each settled movement → `ledger.balance.updated` (key `walletId`), plus `LEDGER_BALANCE_UPDATED` / `WALLET_TIER_CHANGED` events.
- Changing tier bands does **not** recalculate all wallets — the next LP movement does.

---

## 6. Run it locally

```bash
# Postgres :5433, DB ledger-engine
mvn spring-boot:run                     # http://localhost:8080 · /actuator/health
./scripts/bootstrap-runtime.sh          # seed Door + Brain defaults
./scripts/e2e-smoke.sh                  # verify the pipeline
mvn test

# Docker (with simulator)
cp .env.example .env
docker compose --profile simulator up --build
```

Go-live order: engine + Postgres → `bootstrap-runtime.sh` (Door+Brain defaults) → optional `POST /wallets/batch` onboarding → point POS/OMS at the webhook or Kafka.

---

*Deep-dive: [BOOKLET.md](BOOKLET.md). Code anchors: `ApplyPostingUseCase` · `IngestTransactionUseCase` · `FactorMatcher` · `DtoWrapper`.*
