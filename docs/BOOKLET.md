# LedgeRX Booklet

**Single product & engineering document.**  
Do not add parallel topic files — extend this booklet.

| | |
|--|--|
| **Product** | LedgeRX |
| **Module** | `ledger-engine` |
| **Admin** | `ledger-engine-admin-portal` |
| **SDK** | `ledger-engine-sdk` (optional JAR, not Maven Central) |
| **Deploy** | In-cluster / client VPC |
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
11. [Webhook & Kafka ingest](#11-webhook--kafka-ingest)
12. [Critical path — CC_TXN earn + refund](#12-critical-path--cc_txn-earn--refund)
13. [SDK](#13-sdk)
14. [UA use-case recipes](#14-ua-use-case-recipes)
15. [Non-financial engagement](#15-non-financial-engagement)

**D · Ops**
16. [Local run](#16-local-run)
17. [Admin](#17-admin)
18. [Simulator](#18-simulator)
19. [Layout & conventions](#19-layout--conventions)
20. [Decks](#20-decks)
21. [Freeze & debt](#21-freeze--debt)
22. [Quick curls](#22-quick-curls)

---

# A · Product

## 1. What & stack

LedgeRX is the **points / multi-currency wallet system of record**.

- Upstream sends **what happened**
- **Door** admits the event
- **Brain** scores points (rules, no redeploy)
- **Books** post balances (same-currency double-entry)

```text
LedgeRX
├── ledger-engine              API + posting
├── ledger-engine-admin-portal Ops UI
└── ledger-engine-sdk          Optional client JAR
```

| | |
|--|--|
| Java / Boot | 17 / 3.5.x |
| DB | PostgreSQL (local often `:5433`, DB `ledger-engine`) |
| Schema | `JPA_DDL_AUTO=create` pre-UAT; Flyway later |
| API envelope | `R.success` / `Result` (`com.altech.core`) |
| Errors | `BizException` + domain `*ErrorResponse` |
| JSON | camelCase; money amounts as currency-scaled **strings** |

Ledger core only — not payment rails, identity/CRM, compliance UI, or settlement orchestration.

---

## 2. Core model

| Rule | |
|------|--|
| **1 `ownerId` → 1 Wallet** | Hard product rule (CRM CUST id) |
| **Multi-ccy** | Accounts under that wallet (settlement HKD + LP) |
| **No AccountSet / no subAccount** | Removed |
| **Standalone** | No hardcoded client seed in engine |
| **COA** | Internal Finance structure — not a public API concern |
| **Balances** | Live on `account` (`ledgerBalance`, `availableBalance`) under lock |
| **Book display name** | `{ownerId}-{iso}` computed on GET (not stored) |

```text
ownerId 01A31658334  →  Wallet  →  01A31658334-HKD   (01-01-01 settlement)
                                 →  01A31658334-LP    (01-01-01 loyalty)
ownerId HOUSE        →  House   →  01-02-01 operating (earn counterparty)
```

**fullNumber** (digit-only, no English keys):

```text
entity(2) + type(2) + subType(2) + mainAccount + buffer(2) + currency(3)
example    01 + 01 + 01 + 908951901284 + 00 + 344   → member HKD book
```

Account uniqueness: `entity + type + subType + mainAccount + buffer + currency`.  
Every customer `mainAccount` tree includes the settlement (HKD) twin.

| Type | Examples |
|------|----------|
| **FIAT** | `HKD` (4 dp), `USD` (2 dp), … |
| **LOYALTY_POINT** | `LP` (0 dp, DOWN) |
| **CRYPTO** | `BTC`, `USDT`, … |

Customer identity and names live in CRM; ledger stores **`ownerId`** plus optional `vanityCode`.

---

## 3. End-to-end pipeline

```text
TransactionalEvent  (REST or Kafka — same JSON)
      │
      ▼
┌─────────────┐
│ Door        │  isEnabled + entryFactors
│ ingest-     │  optional auto-wallet (HKD + LP)
│ policies    │
└──────┬──────┘
       │ entered
       ▼
┌─────────────┐
│ Brain       │  whenFactors + legacy filters
│ digestion-  │  formula → points + resultCurrency
│ rules       │  first bingo (priority ASC, id ASC)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Accounting  │  AccountingRuleExecution walk
│ rules       │  CR/DR onto COA (same currency)
└──────┬──────┘
       │
       ▼
 ApplyPostingUseCase → movements + DE legs + balances
```

Go-live:

```text
Step 0  Engine + Postgres
Step 1  bootstrap-runtime.sh  → Door + Brain defaults
Step 2  Optional CRM  POST /wallets/batch
Step 3  POS / OMS → webhook or Kafka
```

If Phase 2 runs without a wallet and **auto-wallet off** → `NO_WALLET`.  
If **auto-wallet on** (default after bootstrap) → first eligible event creates HKD+LP in the same TX.

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

Door, Brain, and Accounting all key off the **same** `eventType` token (`CC_TXN`, `CC_CIP`, `CC_SIP`, `LN_TXN`).  
`action` is a **second** field (how to book this fire). Omit on the first spend.

---

## 5. Factors

Shared matcher on Door + Brain (`FactorMatcher` / `FactorSpec`).

### Leaf

```json
{ "id": "F1", "field": "currency", "op": "eq", "value": "HKD" }
{ "field": "mcc", "op": "in", "value": ["5411"] }
{ "field": "amount", "op": "gte", "value": 100 }
{ "field": "metadata.channel", "op": "eq", "value": "POS" }
{ "field": "metadata.merchantName", "op": "startsWith", "value": "MTR" }
{ "field": "metadata.merchantName", "op": "endsWith", "value": "LTD" }
{ "field": "metadata.merchantName", "op": "contains", "value": "MTR" }
```

**Fields:** `currency` · `mcc` · `amount` · `ageDays` · `eventType` · `metadata.*`  
**Ops:** `eq` `neq` `in` `nin` `gt` `gte` `lt` `lte` `between` `exists` `startsWith` `endsWith` `contains`  
(string ops are case-insensitive; `value` may be a list)

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

Demo: 500 HKD × 1% → **5 LP**. Reward currency = Brain `resultCurrency` (`LP` loyalty / `HKD` cashback).

---

## 7. Posting

**All balance writes** go through:

```text
PostingCommand + PostingIntent
  → ApplyPostingUseCase.execute
  → Shooter → MovementBus → execution → accounts
```

| Intent | Member | Counterparty |
|--------|--------|--------------|
| DEPOSIT | + | single-sided |
| WITHDRAWAL | − | single-sided |
| IN_WALLET_TRANSFER | ± | other wallet |
| EARN | + | HOUSE operating DE |
| BURN | − | HOUSE operating DE |
| HOLD / RELEASE | available only | ledger unchanged |
| ADJUSTMENT_REFUND | reverse original legs | same books |

```java
applyPostingUseCase.execute(PostingCommand.earn(...));
applyPostingUseCase.execute(PostingCommand.deposit(...));
```

| ❌ | |
|----|--|
| Earn via deposit API | breaks HOUSE DE |
| Bypass `ApplyPostingUseCase` | drift |

Loyalty DE (CREDIT = ADD, DEBIT = SUBTRACT). House operating books allow negative.

```text
EARN  DR HOUSE  01-02-01  /  CR member 01-01-01   (same currency)
BURN  DR member 01-01-01  /  CR HOUSE  01-02-01
```

T-accounts: **DR left / CR right**. Legs stored with positive amounts + `MovementDirection`.

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

Customer custodian **01-01-01**. House operating **01-02-01** `mainAccount` 9999. House expense **01-04-02**. House wallet `ownerId` **HOUSE** (canonical; legacy `PROGRAM` renamed in place).

### Operator mapping

**Default: `code` ≡ `transactionCode` ≡ webhook `eventType`.**  
Only set `transactionCode` differently if you need to extend later.

| Column | |
|--------|--|
| **`code`** | Profile id **and** (by default) eventType |
| `transactionCode` | Optional override; blank → same as `code` |
| `currency` | Points book (default LP) |
| entity/type/subType/buffer | fullNumber segments |

```text
eventType CC_TXN
    → coa_profile.code OR transaction_code
    → segments + currency
    → AccountingRuleExecution walk
```

Create (simplest):

```json
{
  "code": "CC_TXN",
  "name": "CC spend → LP",
  "entity": "01",
  "type": "01",
  "subType": "01",
  "currency": "LP"
}
```

**Product does not take COA segments on every txn** — only `eventType` (= code).

| Layer | Example |
|-------|---------|
| Outer | `eventType=CC_TXN`, `ownerId` |
| Mid | Accounting rule atoms |
| Inner | Custodian / operating segments |

| API | `/coa-profiles` · `/coa-dictionary` · `/corporate-coa` |
| Onboard | `coaProfileCode` optional |
| Wallet | stamps profile at create |

---

# C · Integration

## 10. API surface

### Product

| Area | Path |
|------|------|
| Wallet | `POST/GET /wallets` · `/wallets/batch` · `/wallets/{ownerId}` |
| Money | `/movements/deposits` · `withdrawals` · `transfers/in-wallet` |
| Hold | `/wallets/holds` · `/releases` |
| History | `/wallets/{ownerId}/movements` · as-of |
| Refund | `POST /movements/{id}/refund` |
| Door | `/ingest-policies` |
| Brain | `/digestion-rules` |
| Webhook | `/integrations/webhooks/transactions` · `/dry-run` |
| Fail / legs | `/integrations/failed-transactions` · `ledger-entries` |
| COA | `/coa-profiles` · `/coa-dictionary` · `/corporate-coa` |
| Accounting | `/accounting-rules` · `/accounting-rule-executions` |

### Money use cases

`CreateDepositUseCase` · `CreateWithdrawalUseCase` · `CreateInWalletTransferUseCase` → posting intents above.

PO → DTO mapping is **`DtoWrapper` only**.

---

## 11. Webhook & Kafka ingest

Same SDK JSON either way. Both call `IngestTransactionUseCase.execute`.

```http
POST /integrations/webhooks/transactions
POST /integrations/webhooks/transactions/dry-run
```

Kafka (off by default):

| | |
|--|--|
| Enable | `LEDGER_KAFKA_ENABLED=true` |
| Topic | `ledger.transaction.events` |
| Key | `eventId` |
| Group | `ledger-engine` |
| Listener | `TransactionEventKafkaListener` → `JSONUtil.readValue` |

Sample spend: [`docs/samples/transactional-event.sdk.json`](samples/transactional-event.sdk.json)

```json
{
  "eventId": "evt-cc-txn-001",
  "ownerId": "01A31658334",
  "eventType": "CC_TXN",
  "amount": "100.00",
  "currency": "HKD",
  "occurredAt": "2026-09-02T06:40:00Z",
  "mainAccount": "908951901284",
  "metadata": { "mcc": "5411", "channel": "UAF_CC" }
}
```

Aliases: `ownerId` ← `associatedIdentifier` / `userId`.  
`JSONUtil.readValue(String, Class)` unwraps `payload` / `data` / `event` / `body` when that object has `eventId`, and stringifies `metadata` values (`mcc: 5411` → `"5411"`).

Response: `status` · `points` · `matchedRuleCode` · `movementId` · `legs` · `eligibilityTrace`.  
Idempotency: `loyalty-earn-{eventId}` / `loyalty-burn-{eventId}`.

### `action` — booking, not product family

`eventType` stays the product (`CC_TXN` / `CC_CIP` / `CC_SIP` / `LN_TXN`).  
`action` says how to book **this** fire. Omit on the first spend (default `SPEND`).

| `action` | When | Books |
|----------|------|-------|
| `SPEND` | First fire. **Omit the field.** | Door → Brain → DE |
| `REFUND` | Customer refund | Full reverse of `originalEventId`. Skip Door/Brain. Amount on this event is not re-scored. |
| `VOID` | Same-day cancel / never captured | Same reverse as REFUND; remarks `void` |
| `CHARGEBACK` | Issuer dispute | Same reverse as REFUND; remarks `chargeback` (fee later) |
| `PARTIAL` | Refund part of the original | Recognised; fail `ACTION_UNSUPPORTED` (needs this event's amount) |
| `ADJUST` | Tip / amount correction | Recognised; fail `ACTION_UNSUPPORTED` |

Not on `action`: HOLD / RELEASE (REST), deposit / withdraw (rails), BURN / REDEEM (other `eventType`), EXPIRE (batch).

Aliases: `ORIGINAL` / `APPLY` / `NORMAL` → `SPEND`; `REVERSE` → `VOID`; `DISPUTE` → `CHARGEBACK`; `ADJUSTMENT` → `ADJUST`.  
Legacy `eventType=CC_TXN_REFUND` still infers `REFUND`.

Refund sample: [`docs/samples/transactional-event-refund.sdk.json`](samples/transactional-event-refund.sdk.json)

```json
{
  "eventId": "evt-cc-txn-001-refund",
  "ownerId": "01A31658334",
  "eventType": "CC_TXN",
  "action": "REFUND",
  "originalEventId": "evt-cc-txn-001",
  "amount": "100.00",
  "currency": "HKD",
  "mainAccount": "908951901284"
}
```

VOID sample: [`docs/samples/transactional-event-void.sdk.json`](samples/transactional-event-void.sdk.json)  
PARTIAL shape (not booked yet): [`docs/samples/transactional-event-partial.sdk.json`](samples/transactional-event-partial.sdk.json)

Full reverse is idempotent across `REFUND` / `VOID` / `CHARGEBACK` (`movementKey={originalKey}-refund`).

### Movement Kafka (internal bus)

After each **SETTLED** movement, when `LEDGER_MOVEMENT_KAFKA_ENABLED=true`:

| | |
|--|--|
| Topic | `ledger.balance.updated` |
| Key | walletId |
| eventName | `LEDGER_BALANCE_UPDATED` |

Also: `ledger.movement.done`. Inbound execute: `initiated` / `balance-update`.

### Wallet tiering

Config: `GET/PUT /wallet-tier-policies` (Door-shaped, unique `criterion` + `currency`).  
Criterion v1 = **`LEDGER_BALANCE`**: sum of `account.ledgerBalance` for this **`wallet.id` + `currency`** (default LP). No COA stem on the policy.  
GET from admin seeds a **disabled draft**. Settle does **not** insert a policy. Tiering starts when ops **Save as Enabled**.

After each SETTLED movement that touches that book, engine writes **`wallet.tier`** in the **same TX** (not Kafka-only):

```text
applyCommand → legs → AssessWalletTierUseCase → LEDGER_BALANCE_UPDATED (currentTier)
                                           ↘ WALLET_TIER_CHANGED if the band moved
```

Upgrade: `ledgerBalance >= band.upgradeAt`.  
Downgrade: `ledgerBalance < downgradeBelow` (blank → use `upgradeAt`). Refund/void/chargeback reverse LP so they can downgrade. HOUSE skipped. HOLD does not change ledger → no tier change.

Changing bands does **not** recalc every wallet; the next LP movement does.

---

## 12. Critical path — CC_TXN earn + refund

Happy path for a credit-card earn (`eventType=CC_TXN`).

```text
Upstream (POS / OMS / card)
  REST  POST /integrations/webhooks/transactions
  Kafka topic ledger.transaction.events
        └── IngestTransactionUseCase (Door → Brain → books)

1. Door     eventType + MCC/ccy/amount/age → admit?
            no wallet + auto-create on:
              POST /wallets  settlement=HKD
              open 01-01-01 on event.mainAccount → HKD + LP
              book names: {ownerId}-HKD , {ownerId}-LP

2. Brain    same eventType, first bingo
            formula → points + resultCurrency (LP or HKD)

3. Accounting  bound combo walks CR/DR (same currency)

            Txn → LP
              DR  HOUSE  01-02-01 LP
              CR  member 01-01-01 LP   ({ownerId}-LP)

            Txn → HKD
              DR  HOUSE  01-02-01 HKD
              CR  member 01-01-01 HKD  ({ownerId}-HKD)

4. Ledger   ledger_movement + 2 ledger_entry legs
            GET /wallets/{ownerId}
            GET /wallets/{ownerId}/movements
            GET /integrations/ledger-entries?movementId=
```

### What gets created

| Layer | Persist | Notes |
|---|---|---|
| Wallet | `wallet` | `ownerId`, `settlementCurrency=HKD` |
| Member books | `account` 01-01-01 | HKD + LP on `event.mainAccount`; name `{ownerId}-{ccy}` |
| House books | HOUSE wallet 01-02 operating | Counterparty for earn; skip settlement twin |
| Movement | `ledger_movement` | `orderType=EARN`, `status=SETTLED`, `movementKey=loyalty-earn-{eventId}` |
| Legs | `ledger_entry` × 2 | One DEBIT, one CREDIT, same amount, same currency |

### Refund — two ways in, one reverse

Same engine path either way: find the settled earn/burn, post `ADJUSTMENT_REFUND` with DR/CR swapped. Brain is **not** re-scored.

**1 · Ops click**

```http
POST /movements/{id}/refund
```

`{id}` is the **settled EARN** (or BURN) movement.

**2 · Upstream event** (`eventType` stays `CC_TXN`, plus `action` + `originalEventId`)

`REFUND` / `VOID` / `CHARGEBACK` intercept **before** Door so a CC_TXN-only entry gate does not `NOT_ENTERED` the reverse.

| Original | Reverse |
|---|---|
| `orderType=EARN` | `ADJUSTMENT_REFUND` |
| `amount=10` | `amount=-10` |
| `status=SETTLED` | new row `SETTLED`; original → `REFUNDED` |
| CR member / DR house | **DR member / CR house** (same books, same magnitude) |
| — | `associatedLedgerMovementId` → original |

Idempotent: second reverse (click or any of REFUND/VOID/CHARGEBACK) returns the existing row (`movementKey={originalKey}-refund`).  
Net of earn + reverse: balances back to pre-earn numbers.

Admin: wallet history → select SETTLED EARN → **Refund · reverse DR/CR**.

### Skip / fail (not the happy path)

| Code | When |
|---|---|
| `NOT_ENTERED` | Door gates reject (MCC / ccy / amount / age) |
| `NO_WALLET` | No wallet and auto-create off |
| `NO_RULE` / `SKIPPED` | Brain: no first bingo |
| `DUPLICATE` | Same `eventId` already earned (`movementKey` hit) |
| `ACTION_UNSUPPORTED` | `PARTIAL` / `ADJUST` — recognised, not booked yet |
| `NO_ORIGINAL` | Reverse action without `originalEventId` |
| Fail queue | `POST /integrations/failed-transactions/{id}/replay` |

---

## 13. SDK

Product backends may use **ledger-engine-sdk** (Java 17) instead of hand-written HTTP. Manual versioned JAR — not Maven Central.

| Topic | Where |
|-------|--------|
| Overview | [SDK OVERVIEW](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/OVERVIEW.md) |
| JAR delivery | [SDK DELIVERY](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/DELIVERY.md) |
| Client install | [SDK INTEGRATION](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/INTEGRATION.md) |
| Wire contract | SDK `docs/EXPECTED_CONTRACT.md` — do not hand-build JSON |

Handshake: `GET /integrations/sdk-info` (engineVersion, minSdkVersion, features).  
Catalog: `GET /integrations/use-cases?enabledOnly=true`.

SDK: `client.verifyEngine()` · `client.catalog().listUseCasesCached()` · `client.useCases().invoke(...)`.

---

## 14. UA use-case recipes

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
| `CREDIT_REWARD` | EARN (member + / HOUSE −) |
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

## 15. Non-financial engagement

Not a spend / payment. Upstream still sends a **webhook event**; Brain uses **FIXED** points.

| | |
|--|--|
| `eventType` | e.g. `LIKE_FB_PAGE` |
| formula | `{ "type": "FIXED", "value": 5 }` → **5 LP** |
| `amount` | `0` allowed (`@PositiveOrZero`; FIXED is not spend-based) |
| `currency` | any valid (e.g. HKD) — points currency from rule = LP |

```json
{
  "code": "LIKE_FB_PAGE",
  "eventType": "LIKE_FB_PAGE",
  "operation": "EARN",
  "isEnabled": true,
  "priority": 10,
  "minAmount": 0,
  "formula": { "type": "FIXED", "value": 5 },
  "resultCurrency": "LP"
}
```

```json
{
  "eventId": "like-001",
  "ownerId": "01A1",
  "eventType": "LIKE_FB_PAGE",
  "amount": 0,
  "currency": "HKD",
  "occurredAt": "2026-08-20T12:00:00Z",
  "metadata": { "channel": "facebook", "pageId": "ua-finance" }
}
```

Door open + auto-wallet → **+5 LP** on member book (HOUSE DE).

---

# D · Ops

## 16. Local run

```bash
# Postgres :5433 / DB ledger-engine
# docker exec test-db psql -U postgres -c 'CREATE DATABASE "ledger-engine";'

mvn spring-boot:run
# or: mvn clean package && java -jar target/ledger-engine-1.0.0.jar

./scripts/bootstrap-runtime.sh   # Door + Brain defaults
./scripts/e2e-smoke.sh
./scripts/upstream-sim.sh
```

Default: `http://localhost:8080` · health `/actuator/health`.

```bash
mvn test
```

Docker:

```bash
docker compose up --build
cp .env.example .env
docker compose --profile simulator up --build
```

Override DB:

```bash
DB_URL=jdbc:postgresql://host:5432/ledger-engine \
DB_USERNAME=postgres DB_PASSWORD=postgres \
mvn spring-boot:run
```

Admin portal: `LEDGER_ENGINE_URL=http://localhost:8080`

---

## 17. Admin

| Screen | |
|--------|--|
| Door | entryFactors + FactorSet presets |
| Brain | whenFactors + formula (tier/table/cap) |
| Webhook | dry-run + matchedPath |
| Wallets / Chart / Rules / Ingest / Ledger | ops |
| Refund | wallet history → SETTLED EARN/BURN |

Nav groups: Wallets / Chart / Rules / Ingest / Ledger / Rails / Guide.  
Flow strip: Door → Brain → Accounting → Ledger.

---

## 18. Simulator

Generic CRM / integrator simulator (`simulator/`). Client product catalogs are **not** modeled here.

```bash
# from ledger-engine root
cp .env.example .env
docker compose --profile simulator up --build
```

Local Python (engine already on `:8080`):

```bash
cd simulator
pip install -r requirements.txt
SIM_MODE=backfill SIM_LEDGER_BASE_URL=http://localhost:8080 \
  SIM_USER_COUNT=1000 SIM_USER_ID_PREFIX=CUST- python simulator.py
```

| Variable | Default | Meaning |
|----------|---------|---------|
| `SIM_MODE` | `webhook` | `backfill` \| `webhook` \| `kafka` \| `both` |
| `SIM_USER_COUNT` | `5` | Synthetic customers |
| `SIM_USER_ID_PREFIX` | `CUST-` | Prefix for synthetic ids |
| `SIM_CUSTOMER_FILE` | | CSV/JSON/lines of real CRM ids |
| `SIM_CURRENCY` | `LP` | Wallet currency |
| `SIM_BATCH_SIZE` | `500` | Batch size (API max 1000) |
| `SIM_ONBOARD_WALLETS` | `true` | Pre-onboard for event modes |
| `SIM_WEBHOOK_URL` | `http://app:8080/integrations/webhooks/transactions` | |

Re-run is safe: batch returns `alreadyExists`. Missing wallets on event modes → `SKIPPED`.

Production backfill: export CRM ids → `POST /wallets/batch` (≤1000/chunk) against staging, then production. Enable Phase 2 traffic only after counts match.

---

## 19. Layout & conventions

```text
src/main/java/
├── com.altech.core/          # R/Result, BizException, AuditEntity, Currency, JSONUtil
└── com.altech.ledger/
    ├── App.java
    ├── config/
    ├── endpoint/             # *Endpoint
    ├── usecase/              # $ActionVerb$UseCase.execute
    ├── entity/po|dto|enu/
    ├── repository/
    ├── service/              # MovementBus, DtoWrapper
    └── listener/             # optional Kafka
```

| Layer | Convention | Example |
|---|---|---|
| **Endpoint** | `*Endpoint` | `WalletEndpoint` |
| **Use case** | `$ActionVerb$UseCase` + `execute` | `CreateWalletOnboardingUseCase` |
| **DTO** | `Create*RequestDto` / `Get*ResponseDto` | `CreateWalletOnboardRequestDto` |
| **Mapper** | `DtoWrapper` | `DtoWrapper.getLedgerMovementResponseDto` |
| **Logging** | `@Slf4j` | — |
| **JSON** | camelCase | `ownerId`, `settlementCurrency` |

---

## 20. Decks

Under `docs/decks/` (assets only — not a second handbook):

| Prefer | File |
|--------|------|
| **Primary briefing** | `LedgeRX-UAFinance-Full-Briefing.pptx` |
| **Factors** | `LedgeRX-Factor-Playbook.pptx` |
| Sequence | `LedgeRX-UAF-Earn-Process-Burn-Sequence.pptx` + png/html |

---

## 21. Freeze & debt

### Feature freeze (core)

Door/Brain factors · posting · recipes · hold · money rails · Admin path · this booklet.

### Open debt

| ID | |
|----|--|
| TD-SEC-001 | API key (cluster trust OK now) |
| TD-OPS-001 | Flyway |
| TD-API-001 | Legacy `/ledger-wallets` · `/ledger-accounts` · `/accounts` removed |

### Parked

True Expense-GL pool (vs HOUSE operating) · cashback payout rail · stacking · named factor packs · MTD counters.

### Out of scope

Payment gateway / card rails · full CRM master · compliance UI · multi-party settlement orchestration.

---

## 22. Quick curls

```bash
# Wallet — primary HKD + LP book
curl -sS -X POST localhost:8080/wallets -H 'Content-Type: application/json' -d '{
  "ownerId":"01A31658334","settlementCurrency":"HKD","name":"Wilfill Kick",
  "accounts":[{"currency":"LP","name":"Loyalty points","refCode":"LP"}]
}'

# Batch (soft-idempotent, max 1000)
curl -sS -X POST localhost:8080/wallets/batch -H 'Content-Type: application/json' -d '{
  "wallets":[{"ownerId":"01A31658334","settlementCurrency":"HKD","accounts":[{"currency":"LP"}]}]
}'

curl -sS localhost:8080/wallets/01A31658334
curl -sS localhost:8080/ingest-policies
curl -sS localhost:8080/digestion-rules

# Earn (recipe / CC_TXN)
curl -sS -X POST localhost:8080/integrations/webhooks/transactions/dry-run \
  -H 'Content-Type: application/json' -d '{
  "eventId":"t1","ownerId":"01A31658334","eventType":"CC_TXN",
  "amount":100,"currency":"HKD","occurredAt":"2026-09-02T06:40:00Z",
  "mainAccount":"908951901284","metadata":{"mcc":"5411"}
}'

# Money
curl -sS -X POST localhost:8080/movements/deposits -H 'Content-Type: application/json' -d '{
  "ownerId":"01A31658334","currency":"HKD","amount":100
}'

# Refund
curl -sS -X POST localhost:8080/movements/{id}/refund
```

Required onboard: `ownerId`, `settlementCurrency`. Duplicate `ownerId` → `WAL0409`.  
Envelope: `Result` with `data` / `requestId`.

---

*End of booklet. Code: `ApplyPostingUseCase` · `IngestTransactionUseCase` · `FactorMatcher` · `DtoWrapper`.*
