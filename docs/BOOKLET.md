# LedgeRX Booklet

**Single product & engineering document.**  
Do not add parallel topic files under `docs/` — extend this booklet.

| | |
|--|--|
| **Product** | LedgeRX |
| **Module** | `ledger-engine` |
| **Admin** | `ledger-engine-admin-portal` |
| **SDK** | `ledger-engine-sdk` (optional) |
| **Deploy** | In-cluster only |
| **Tagline** | Earn · burn · books — without redeploy |

---

## Contents

**A · Product**
1. [What & stack](#1-what--stack)
2. [Core model](#2-core-model)
3. [End-to-end pipeline](#3-end-to-end-pipeline)

**B · Engine**
4. [Door & Brain](#4-door--brain)
5. [Factors](#5-factors)
6. [Equations](#6-equations)
7. [Posting](#7-posting)
8. [Hold](#8-hold)
9. [COA (internal)](#9-coa-internal)

**C · Integration**
10. [API surface](#10-api-surface)
11. [Webhook](#11-webhook)
12. [UA use-case recipes](#12-ua-use-case-recipes)

**D · Ops**
13. [Local bootstrap](#13-local-bootstrap)
14. [Admin](#14-admin)
15. [Decks](#15-decks)
16. [Freeze & debt](#16-freeze--debt)

---

# A · Product

## 1. What & stack

LedgeRX is the **points / multi-currency wallet system of record**.

- Upstream sends **what happened**
- **Door** admits the event
- **Brain** scores points (rules, no redeploy)
- **Books** post balances (double-entry loyalty + money rails)

```text
LedgeRX
├── ledger-engine              API + posting
├── ledger-engine-admin-portal Ops UI
└── ledger-engine-sdk          Optional client JAR
```

| | |
|--|--|
| Java / Boot | 17 / 3.x |
| DB | PostgreSQL (local often `:5433`) |
| Schema | `JPA_DDL_AUTO=create` pre-UAT; Flyway later |

---

## 2. Core model

| Rule | |
|------|--|
| **1 `ownerId` → 1 Wallet** | Hard product rule |
| **Multi-ccy** | Accounts under wallet (e.g. HKD + LP) |
| **No AccountSet** | Removed |
| **Standalone** | No hardcoded client seed in engine |
| **COA** | Internal Finance structure — not a public API concern |

```text
ownerId 01A1  →  Wallet  →  HKD account
                          →  LP account
ownerId 01A2  →  Wallet  →  …
```

UA sheet: `W_1` ↔ owner `01A1`, `W_2` ↔ `01A2` (aligned with 1:1).

---

## 3. End-to-end pipeline

```text
TransactionalEvent
      │
      ▼
┌─────────────┐
│ Door        │  isEnabled + entryFactors
│ ingest-     │  optional auto-wallet
│ policies    │
└──────┬──────┘
       │ entered
       ▼
┌─────────────┐
│ Brain       │  whenFactors + legacy filters
│ digestion-  │  formula → points
│ rules       │  first match by priority
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Recipe?     │  if eventType ∈ catalog (CC_TXN_*, LOAN_DD_*)
│ else plain  │  EARN / BURN
│ EARN/BURN   │
└──────┬──────┘
       │
       ▼
 ApplyPostingUseCase → movements + DE legs + balances
```

---

# B · Engine

## 4. Door & Brain

| | **Door** | **Brain** |
|--|----------|-----------|
| API | `/ingest-policies` | `/digestion-rules` |
| Job | Admit? Auto-wallet? | Eligible? How many points? |
| Factors | `entryFactors` | `whenFactors` |
| Score | **Never** | Formula JSON |
| Rows | ~1 | N (priority) |

Legacy Brain columns (`minAmount`, ccy, mcc, age) still **AND** with `whenFactors`.

---

## 5. Factors

Shared matcher on Door + Brain.

### Leaf

```json
{ "id": "F1", "field": "currency", "op": "eq", "value": "HKD" }
{ "field": "mcc", "op": "in", "value": ["5411"] }
{ "field": "amount", "op": "gte", "value": 100 }
{ "field": "metadata.channel", "op": "eq", "value": "POS" }
```

**Fields:** `currency` · `mcc` · `amount` · `ageDays` · `eventType` · `metadata.*`  
**Ops:** `eq` `neq` `in` `nin` `gt` `gte` `lt` `lte` `between` `exists`

### FactorSet (boolean)

| `match` | Meaning |
|---------|---------|
| *(array)* | AND all leaves |
| `all` | AND |
| `any` | OR / 1-of-N |
| `atLeast` + `count` | K-of-N |
| `exactly` / `atMost` | count bounds |
| `not` | none |
| `oneOf` | exactly one child |
| `anyGroup` / `allGroups` | group OR / AND |

```json
{ "match": "any", "factors": [F1, F2, F3, F4, F5] }
{ "match": "atLeast", "count": 2, "factors": [F1, F2, F3, F4, F5] }
{
  "match": "anyGroup",
  "groups": [
    { "id": "G12", "factors": [F1, F2] },
    { "id": "G34", "factors": [F3, F4] }
  ]
}
```

**Explain:** `eligibilityTrace[].matchedPath` (e.g. `G12 > F1 > currency:eq`).

---

## 6. Equations

JSON only — **no SpEL**.

| `type` | |
|--------|--|
| `RATE` | amount × rate |
| `FIXED` | constant |
| `LINEAR` | rate×amount + fixed |
| `AMOUNT` | points = amount |
| `TIERED_RATE` | marginal brackets |
| `TABLE` | by metadata key |

Optional on any: `multiplier` · `cap` · `floor`.

```json
{ "type": "RATE", "rate": 0.01, "cap": 50, "floor": 1 }
```

Demo: 500 HKD × 1% → **5 LP**.

---

## 7. Posting

**All balance writes** go through:

```text
PostingCommand + PostingIntent
  → ApplyPostingUseCase.execute
  → Shooter → execution → accounts
```

| Intent | Member | Counterparty |
|--------|--------|--------------|
| DEPOSIT | + | single-sided |
| WITHDRAWAL | − | single-sided |
| IN_WALLET_TRANSFER | ± | other wallet |
| EARN | + | PROGRAM pool DE |
| BURN | − | PROGRAM pool DE |
| HOLD / RELEASE | available only | ledger unchanged |

```java
applyPostingUseCase.execute(PostingCommand.earn(...));
applyPostingUseCase.execute(PostingCommand.deposit(...));
```

| ❌ | |
|----|--|
| Earn via deposit API | breaks PROGRAM DE |
| Bypass `ApplyPostingUseCase` | drift |

Loyalty DE:

```text
EARN  DR PROGRAM.LP / CR member.LP
BURN  DR member.LP  / CR PROGRAM.LP
```

---

## 8. Hold

| | ledger | available |
|--|--------|-----------|
| EARN | ↑ | ↑ |
| HOLD | — | ↓ |
| RELEASE | — | ↑ (≤ ledger) |
| BURN | ↓ | ↓ |

```http
POST /wallets/holds
POST /wallets/releases
```

---

## 9. COA (internal)

Finance may use Entity / AccountType / SubType / full numbers (**inner gem**).

**Product does not take COA on every txn.**

| Layer | Example |
|-------|---------|
| Outer | `eventType=CC_TXN_LP`, `ownerId` |
| Mid | Posting recipe atoms |
| Inner | Custodian / Expense segments (GL export later) |

| API | `/coa-profiles` |
| Onboard | `coaProfileCode` optional → DEFAULT |
| Wallet | stamps profile at create |

Phase-1 engine DE still uses **PROGRAM pool**; labels map to Finance COA outside or later.

---

# C · Integration

## 10. API surface

### Product

| Area | Path |
|------|------|
| Wallet | `/wallets` |
| Money | `/movements/deposits` · `withdrawals` · `transfers/in-wallet` |
| Hold | `/wallets/holds` · `/releases` |
| History | `/wallets/{ownerId}/movements` · as-of |
| Door | `/ingest-policies` |
| Brain | `/digestion-rules` |
| Webhook | `/integrations/webhooks/transactions` · `/dry-run` |
| Fail / legs | `/integrations/failed-transactions` · `ledger-entries` |
| COA | `/coa-profiles` |

### Legacy (deprecated)

`/ledger-wallets` · `/ledger-accounts` · `/rules` — do not use for new work.

### Money use cases

`CreateDepositUseCase` · `CreateWithdrawalUseCase` · `CreateInWalletTransferUseCase` → posting intents above.

---

## 11. Webhook

```http
POST /integrations/webhooks/transactions
POST /integrations/webhooks/transactions/dry-run
```

```json
{
  "eventId": "txn-1",
  "ownerId": "01A1",
  "eventType": "CC_TXN_LP",
  "amount": 100,
  "currency": "HKD",
  "occurredAt": "2026-08-20T12:00:00Z",
  "metadata": { "mcc": "5411", "useCase": "CC_TXN_LP" }
}
```

Response: `status` · `points` · `matchedRuleCode` · `movementId` · `legs` · `eligibilityTrace` (+ `matchedPath`).

Idempotency: `loyalty-*` / `loyalty-recipe-{eventId}-…` movement keys.

---

## 12. UA use-case recipes

Sheet use cases → **recipe codes** → **atoms** → posting.

```text
eventType | metadata.useCase
    → PostingRecipeCatalog
    → ApplyPostingRecipeUseCase
    → ApplyPostingUseCase
```

### Atoms

| Atom | Phase-1 books |
|------|----------------|
| `CREDIT_REWARD` | EARN (member + / PROGRAM −) |
| `REDEEM` | BURN |
| `CASHBACK` | BURN (payout rail later) |
| `CONVERT_HKD_TO_LP` | BURN HKD + EARN LP (1:1) |

### CC recipes

| code | atoms |
|------|--------|
| `CC_TXN_HKD` | CREDIT @ HKD |
| `CC_TXN_LP` | CREDIT @ LP |
| `CC_TXN_HKD_REDEEM` | CREDIT + REDEEM |
| `CC_TXN_HKD_CASHBACK` | CREDIT + CASHBACK |
| `CC_TXN_LP_REDEEM` | CREDIT + REDEEM @ LP |
| `CC_TXN_LP_CASHBACK` | CREDIT + CASHBACK @ LP |
| `CC_TXN_HKD_TO_LP` | CREDIT HKD + CONVERT |
| `CC_TXN_HKD_LP_REDEEM` | CREDIT + CONVERT + REDEEM |
| `CC_TXN_HKD_LP_CASHBACK` | CREDIT + CONVERT + CASHBACK |

### Loan recipes

`LOAN_DD_*` same patterns. Sheet **Load DD** → alias `LOAD_DD_*` → loan codes.

Brain: `eventType` should match code (formula still sets **points**).  
Unknown types (e.g. `PURCHASE`) → classic EARN/BURN only.

---

# D · Ops

## 13. Local bootstrap

```bash
# Postgres :5433 / DB ledger-engine
mvn spring-boot:run

./scripts/bootstrap-runtime.sh   # if present
./scripts/e2e-smoke.sh
./scripts/upstream-sim.sh
```

Admin: `LEDGER_ENGINE_URL=http://localhost:8080`

---

## 14. Admin

| Screen | |
|--------|--|
| Door | entryFactors + FactorSet presets |
| Brain | whenFactors + formula (tier/table/cap) |
| Webhook | dry-run + matchedPath |
| Demo / COA / wallets / holds / review | ops |

---

## 15. Decks

Under `docs/decks/` (assets only — not a second handbook):

| Prefer | File |
|--------|------|
| **Primary briefing** | `LedgeRX-UAFinance-Full-Briefing.pptx` |
| **Factors** | `LedgeRX-Factor-Playbook.pptx` |
| Sequence | `LedgeRX-UAF-Earn-Process-Burn-Sequence.pptx` + png/html |

Other pptx may be aliases / older cuts of the same story.

---

## 16. Freeze & debt

### Feature freeze (core)

Door/Brain factors · posting · recipes · hold · money rails · Admin path · this booklet.

### Open debt

| ID | |
|----|--|
| TD-SEC-001 | API key (cluster trust OK now) |
| TD-OPS-001 | Flyway |
| TD-API-001 | Remove legacy `/ledger-*` when idle |

### Parked

True Expense-GL pool (vs PROGRAM label) · cashback payout rail · stacking · named factor packs · MTD counters.

---

## Quick curls

```bash
# Wallet
curl -sS -X POST localhost:8080/wallets -H 'Content-Type: application/json' -d '{
  "ownerId":"01A1","settlementCurrency":"HKD","accounts":[{"currency":"LP"},{"currency":"HKD"}]
}'

# Door / Brain
curl -sS localhost:8080/ingest-policies
curl -sS localhost:8080/digestion-rules

# Loyalty (recipe)
curl -sS -X POST localhost:8080/integrations/webhooks/transactions/dry-run \
  -H 'Content-Type: application/json' -d '{
  "eventId":"t1","ownerId":"01A1","eventType":"CC_TXN_LP",
  "amount":100,"currency":"HKD","occurredAt":"2026-08-20T12:00:00Z"
}'

# Money
curl -sS -X POST localhost:8080/movements/deposits -H 'Content-Type: application/json' -d '{
  "ownerId":"01A1","currency":"HKD","amount":100
}'
```

---

*End of booklet. Code: `ApplyPostingUseCase` · `ApplyPostingRecipeUseCase` · `PostingRecipeCatalog` · `FactorMatcher`.*
