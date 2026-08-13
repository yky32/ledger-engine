# LedgeRX — Product Overview

**LedgeRX** is the business product name for our standalone, deployable **wallet + ledger** platform
(engineering module: `ledger-engine`). It supports custodial wallets (deposits, withdrawals, transfers),
loyalty points/credits, and full double-entry accounting — especially for **credit card / issuer** and
retail loyalty programmes.

Clients deploy it in their own environment (on-prem, private cloud, or VPC), configure Door + Brain rules
at runtime, and integrate via a stable HTTP API. LedgeRX enforces double-entry rules, immutable history, and
idempotent posting so downstream systems can treat it as the financial source of truth.

Brand: [docs/BRAND.md](docs/BRAND.md) · Setup: [README.md](README.md) · Integration: [INTEGRATION.md](INTEGRATION.md).

**Java SDK:** clients may receive **ledger-engine-sdk** as a versioned JAR under contract (manual email delivery — not Maven Central). See [SDK OVERVIEW](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/OVERVIEW.md) and [SDK DELIVERY](https://github.com/yky32/ledger-engine-sdk/blob/main/docs/DELIVERY.md).

---

## Product summary

**LedgeRX** is a **standalone wallet + ledger product** built as a Spring Boot service (`ledger-engine`).
It supports loyalty points, custodial wallets, deposits, withdrawals, transfers, and full audit-grade
double-entry accounting.

Clients deploy it in their own infrastructure and integrate over HTTP (and optionally Kafka).

| Aspect | Description |
|---|---|
| **Product name** | **LedgeRX** |
| **Purpose** | System of record for wallet balances and every movement between accounts |
| **Model** | Chart of Accounts (COA) + double-entry legs — programme pool + member books |
| **Customer unit** | `Wallet` (1 `ownerId` → 1 wallet) + multi-ccy accounts (e.g. HKD + LP) |
| **Operations** | **Earn / Burn / Process** (loyalty) + **Deposit / Withdrawal / Transfer** + **Hold/Release** |
| **Go-live** | **Phase 1** onboard wallets from CRM → **Phase 2** ingest transactional events |
| **Guarantees** | Double-entry, idempotent posting, append-only history, reversal support |

**Standalone:** no shared foundation JAR required; pure Spring Boot with an embedded core package.

**In scope (product vision):** wallets, movements, deposits, withdrawals,
transfers, virtual accounts, recipients, FX, compliance workflows, Kafka pipeline — implemented on a clean
double-entry core.

**Client-owned (integrate at boundary):** identity/KYC provider, card acquirer, bank rails, campaign UI.

---

## Successor to Ledger Engine replaces  as the **single standalone product** you ship to enterprise
clients. Same product domain; different (better) accounting foundation.

| Dimension | (legacy) | Ledger Engine (standalone) |
|---|---|---|
| **Deployment** | Shared multi-tenant platform | Client self-hosted (on-prem / VPC) |
| **Dependencies** | Shared platform libraries | Pure Spring Boot — embedded `com.altech.core` |
| **Balance storage** | Mutable `ledgerBalance` / `availableBalance` columns | **Derived** from `journal_entry` (hold via COA sub-accounts) |
| **Wallet model** | `Wallet` + `Account` (Snowflake IDs) | `Wallet` + `ledger_account` (UUID) |
| **Movement log** | `LedgerMovement` + Kafka pipeline | `ledger_movement` + journal link (Kafka optional) |
| **Accounting** | Rule legs partial; balance updated directly | Full double-entry on every movement |
| **Loyalty** | Not primary | First-class Earn / Burn / Process + rules engine |

### Design upgrade (not a port)

Capabilities from are **reimplemented**, not copied:

```text
Legacy: movement → mutate account.ledgerBalance
Engine: movement → journal_transaction → derive balance
```

Hold / available split uses **COA hold accounts** (`hold:{ownerId}:{currency}`) instead of a second balance
column — same product behaviour, audit-grade history.

---

## Capability parity matrix

Status key: **Shipped** | **Partial** | **Planned**

| Domain | capability | Ledger Engine | Status |
|---|---|---|---|
| **Wallet** | Create, activate, status, my-wallets, ext lookup | `POST /wallets`, `GET /wallets`, batch CRM import | **Partial** (no activation/IDV yet) |
| **Account / COA** | Multi-currency sub-accounts, COA segments | `ledger_account` + templates; pools on startup | **Partial** |
| **Balance** | ledger + available columns | Derived balance + hold accounts (planned) | **Partial** |
| **Deposit** | Bank + card (GrandPay) + manual | `POST /movements/deposits` (AUTO/MANUAL) | **Partial** (no card rail yet) |
| **Withdrawal** | Linked bank target | `POST /movements/withdrawals` | **Partial** (no linked bank entity yet) |
| **Transfer** | In-wallet + SWIFT + docs | `POST /movements/transfers/in-wallet` | **Partial** |
| **Movement query** | By id, my-movements, admin list | `GET /movements/{id}`, `GET /movements?walletId=` | **Shipped** |
| **Movement workflow** | AUTO / MANUAL, admin settle | AUTO + `PUT /movements/{id}/settle` | **Partial** |
| **Earn / Burn** | Via order types / rules | Rule engine + webhook/Kafka ingestion | **Shipped** |
| **Process** | Hold, expire, adjust | **HOLD/RELEASE shipped**; EXPIRE planned | **Partial** |
| **Virtual accounts** | Application + approval flow | — | **Planned** |
| **Recipients** | CRUD + auto-save on transfer | — | **Planned** |
| **Linked bank accounts** | CRUD for withdrawal targets | — | **Planned** |
| **Payment methods** | Card/bank per wallet | — | **Planned** |
| **FX rates** | CRUD + wallet view conversion | CRUD endpoints | **Partial** |
| **Accounting rules** | Rule + RuleExecution per OrderType | Legacy `/rules`; **loyalty earn uses `digestion_rule` DB** (not YAML) | **Partial** |
| **Compliance** | Movement compliance context + files | Movement metadata field | **Planned** |
| **Kafka pipeline** | MOVEMENT_INITIATED → BALANCE_UPDATE → DONE | Transaction events consumer; movement events planned | **Partial** |
| **Webhooks inbound** | Activation + deposit callback | **Transaction webhook + digestion + DE legs** | **Shipped** (loyalty path) |
| **Webhooks outbound** | Wallet activation to core | — | **Planned** |
| **Auth / tenancy** | OAuth2 JWT + tenant filter | Client gateway (optional module planned) | **Planned** |
| **Dashboard** | Stub | — | **Planned** |

---

## Domain model & relationships

The product is built around **five persistent domain entities**. Journal entries remain the source of truth;
wallets and movements are the **product layer** clients interact with (matching surface).

### Core entities

| Entity | Table | Role |
|---|---|---|
| **Wallet** | `wallet` | Product-facing wallet — owner, alias, status, link to ledger account |
| **LedgerAccount** | `ledger_account` | COA account — customer wallet, pools, clearing, hold buckets |
| **LedgerMovement** | `ledger_movement` | Business operation log — deposit, withdrawal, transfer, earn, burn |
| **JournalTransaction** | `journal_transaction` | Atomic double-entry posting with idempotency key |
| **JournalEntry** | `journal_entry` | Debit or credit leg linking a transaction to an account |

### Entity relationships

```text
wallet ledger_movement
┌──────────────────┐ ┌─────────────────────────┐
│ id │ │ id │
│ account_id ──────┼──▶ ledger_account│ movement_key (unique) │
│ owner_id │◀──┐ │ wallet_id │
│ alias, status │ │ │ order_type, status, mode│
└──────────────────┘ │ │ journal_transaction_id ─┼──▶ journal_transaction
 │ └─────────────────────────┘
ledger_account │ │
┌──────────────────┐ │ ▼
│ id (PK) │◀──┘ journal_entry (debit/credit legs)
│ external_reference
│ type (COA), currency
└──────────────────┘
```

| Relationship | Cardinality | Meaning |
|---|---|---|
| `JournalTransaction` → `JournalEntry` | 1 : many | Every transaction has ≥ 2 legs; debits = credits per currency |
| `LedgerAccount` → `JournalEntry` | 1 : many | An account accumulates history through its entries |
| `JournalTransaction` → `JournalTransaction` | 0..1 reversal | A reversal transaction points at the original via `reversal_of_id` |

**Balance is not stored.** A wallet balance is **derived** at query time:

```text
balance(account) = Σ CREDIT amounts − Σ DEBIT amounts (for that account's currency)
```

### Account roles (same entity, different purpose)

All accounts share the `ledger_account` table. Role is determined by `type` + `external_reference`:

| Role | `type` | `external_reference` pattern | Created by |
|---|---|---|---|
| **Customer wallet** | `LIABILITY` | `wallet:{associatedIdentifier}:{currency}` | Phase 1 — `POST /wallets` |
| **Issuance / expense pool** | `EXPENSE` | `pool:loyalty-expense:{currency}` | Optional — create via account API if needed |
| **Outstanding liability pool** | `LIABILITY` | `pool:loyalty-liability:{currency}` | Optional — create via account API if needed |
| **Deposit clearing** | `ASSET` | `pool:clearing-deposit:{currency}` | Optional — create via account API if needed |
| **Withdrawal clearing** | `ASSET` | `pool:clearing-withdrawal:{currency}` | Optional — create via account API if needed |
| **Held balance** (planned) | `ASSET` | `hold:{userId}:{currency}` | Process HOLD operation |

```text
Customer (client CRM) Program (Ledger Engine)
 │ │
 │ 1 : 1 │ 1 pool per currency
 ▼ ▼
 wallet:CUST-001:LP pool:loyalty-expense:LP
 (LIABILITY) pool:loyalty-liability:LP
 │ │
 └──────── journal_entry ───────┘
 (Earn / Burn legs)
```

**One customer → one wallet** is enforced by unique `external_reference` on `ledger_account`.

### Operation → domain mapping

| Product operation | Domain artifact | Typical legs |
|---|---|---|
| **Deposit** | `ledger_movement` + journal | Debit clearing-deposit pool; credit wallet |
| **Withdrawal** | `ledger_movement` + journal | Debit wallet; credit clearing-withdrawal pool |
| **In-wallet transfer** | `ledger_movement` + journal | Debit source wallet; credit target wallet |
| **Earn** | `journal_transaction` + 2 `journal_entry` | Debit expense pool; credit wallet |
| **Burn** | `journal_transaction` + 2 `journal_entry` | Debit wallet; credit liability pool |
| **Process** | `journal_transaction` + N `journal_entry` | Rule-specific (e.g. ADJUST) |
| **Reversal** | New `journal_transaction` with `reversal_of_id` | Mirror legs of original |

Ingestion path (Phase 2):

```text
TransactionalEvent
 │
 ▼
TransactionRuleEngine → RuleDecision (EARN | BURN | PROCESS, points, currency)
 │
 ▼
WalletOnboardingService.walletRef(userId) → lookup ledger_account
 │
 ▼
LedgerService.post() → journal_transaction + journal_entry rows
```

### Application layers

| Layer | Package | Responsibility |
|---|---|---|
| **API** | `api` | REST: accounts, transactions, reversals |
| **Onboarding** | `onboarding` | Wallet entity, CRM batch import, program pool bootstrap |
| **Movement** | `movement` | Deposits, withdrawals, transfers, movement settlement |
| **Integration** | `integration` | Webhook/Kafka ingestion, loyalty rule engine |
| **Application** | `application` | Posting rules, balance derivation, idempotency, locking |
| **Domain** | `domain` | `LedgerAccount`, `JournalTransaction`, `JournalEntry` |
| **Infrastructure** | `infrastructure` | JPA repositories, SQL balance aggregation |

External systems never write journal rows directly — they call onboarding or integration APIs; the
application layer enforces COA rules and double-entry integrity.

---

## Expanded product epics

Building on the four loyalty epics below, standalone product capabilities adds **Epics 5–12**:

| Epic | Domain | equivalent |
|---|---|---|
| **5** | Deposits & withdrawals | `DepositEndpoint`, `WithdrawalEndpoint` |
| **6** | Transfers (in-wallet, SWIFT) | `WalletTransferEndpoint`, `SwiftTransferEndpoint` |
| **7** | Movement workflow | AUTO/MANUAL, admin settle, compliance |
| **8** | Virtual accounts | `VirtualAccountEndpoint` |
| **9** | Recipients & linked banks | `RecipientsEndpoint`, `LinkedBankAccountEndpoint` |
| **10** | FX & payment methods | `FxRateEndpoint`, `PaymentMethodEndpoint` |
| **11** | Kafka movement pipeline | `MOVEMENT_INITIATED` → `BALANCE_UPDATE` → `DONE` |
| **12** | Auth, tenancy, webhooks | OAuth2, tenant filter, outbound callbacks |

---

When an enterprise **purchases and deploys** Ledger Engine, integration happens in **two phases**.
Phase 2 must not start until Phase 1 is complete.

```text
PHASE 1 — Wallet onboarding (CRM / legacy system)
═══════════════════════════════════════════════════
Client CRM or membership DB
 │
 │ export customers (userId, name, …)
 ▼
POST /wallets ← one customer at signup
POST /wallets/batch ← bulk import at go-live
 │
 ▼
ledger_account per customer (wallet:{associatedIdentifier}:LP)
 │
 ▼
Client confirms: "all customers onboarded" ✓


PHASE 2 — Transaction processing (runtime)
══════════════════════════════════════════
POS / e-commerce / campaign system
 │
 │ webhook or Kafka event (eventId, userId, eventType, amount)
 ▼
POST /integrations/webhooks/transactions
 or Kafka: ledger.transaction.events
 │
 ▼
Rule check → Earn / Burn / Process
 │
 ▼
Points posted (only if wallet exists from Phase 1)
```

| Phase | When | Who acts | Ledger API |
|---|---|---|---|
| **1. Open wallets** | Go-live / CRM sync | Client IT reads legacy CRM | `POST /wallets`, `/wallets/batch` |
| **2. Process events** | Live operations | Client transactional systems | Webhook + Kafka |

If Phase 2 starts before Phase 1 finishes, events for missing customers return **`SKIPPED`**
(`Wallet not onboarded`) — no silent wallet creation.

### Phase 1 example — bulk CRM import

```http
POST /wallets/batch
Content-Type: application/json

{
 "wallets": [
 { "associatedIdentifier": "CRM-0001", "currency": "LP", "name": "Alice" },
 { "associatedIdentifier": "CRM-0002", "currency": "LP", "name": "Bob" }
 ]
}
```

Response:

```json
{
 "requested": 2,
 "created": 2,
 "alreadyExists": 0,
 "createdWallets": [ ... ],
 "alreadyExistingAssociatedIdentifiers": []
}
```

Re-running the same batch is safe — existing wallets are counted in `alreadyExists`.

### Phase 2 example — shoot transactional event

```http
POST /integrations/webhooks/transactions
Content-Type: application/json

{
 "eventId": "pos-20260805-001",
 "userId": "CRM-0001",
 "eventType": "PURCHASE",
 "amount": 150.00,
 "currency": "LP"
}
```

---

## Enterprise product epics

When an enterprise adopts Ledger Engine, they are not just deploying a database — they are standing up a
**loyalty ledger program**. The work breaks into four epics. **Epic 1 is foundational**: nothing else works
without a wallet per user.

```text
Epic 1 User Creation ──▶ Wallet Provisioning (foundation)
 │
 ▼
Epic 2 Earn / Burn / Process on wallet balance (ledger core — shipped)
 │
 ├─▶ Epic 3 Tier & ranking (client rules — reads balance)
 │
 └─▶ Epic 4 Process rules engine (client pluggable — hold/expire/settle)
```

### Epic 1 — Wallet onboarding (product setup) ⭐

**Phase 1 of go-live.** Client opens a wallet for each customer in their legacy CRM **before** sending
transactional events.

| Trigger | API |
|---|---|
| New customer at signup | `POST /wallets` |
| Go-live / CRM bulk sync | `POST /wallets/batch` |

On startup, the product ensures **program COA pools** exist (expense + liability per currency).

| Responsibility | Owner |
|---|---|
| Customer master data (CRM) | Client legacy system |
| **Open wallet per customer** | Client → Ledger Engine Phase 1 |
| Program pool accounts | Not auto-seeded (create via API if product needs pools) |
| Store balance truth | Ledger Engine |

Transaction processing (Phase 2) **never creates wallets**. Missing wallet → event skipped.

---

### Epic 2 — Transaction processing (Earn / Burn / Process)

**Phase 2 of go-live.** Starts only after Phase 1 wallets exist.

External systems shoot events (webhook / Kafka). Ledger Engine:

1. **Rule check** — match `eventType`, thresholds, operation
2. **Earn / Burn / Process** — post journal if wallet exists

| Operation | Trigger example | Effect |
|---|---|---|
| **Earn** | `PURCHASE` | Credit user wallet |
| **Burn** | `REDEEM` | Debit user wallet |
| **Process** | `ADJUST`, `HOLD`, … | Rule-specific lifecycle |

**Ledger Engine owns rule execution and posting.** External systems only send events.

---

### Epic 3 — Tier & ranking

When a user reaches a certain balance or lifetime earn threshold, the client promotes them to a higher
**tier** (Silver → Gold → Platinum).

```text
Ledger Engine Client tier service
 │ │
 │ GET /accounts/{id}/balance │
 │ GET /accounts/{id}/entries │
 └──────────────────────────────────▶│ evaluate rules
 │ update tier / ranking
 │ trigger perks (outside ledger)
```

| Responsibility | Owner |
|---|---|
| Points balance & history | **Ledger Engine** |
| Tier thresholds, ranking logic, perks | **Client** (or future tier module) |
| When to re-evaluate tier | Client (on earn, scheduled job, or event) |

Ledger Engine does **not** store tier today. It provides the **balance truth** that tier rules read from.

---

### Epic 4 — Process rules engine

**Process** rules are configured alongside Earn/Burn in `ledger.integration.rules`:

```yaml
- event-type: ADJUST
 operation: PROCESS
 process-type: ADJUST
 formula: FIXED:10
```

Different enterprises plug different rule sets. Ledger Engine executes matching **Process** postings;
advanced hold/expire/settle flows are extended per `process-type`.

| Responsibility | Owner |
|---|---|
| Rule definitions (`eventType`, thresholds, operation) | Config / client |
| Rule check + Earn / Burn / Process posting | **Ledger Engine** |
| Wallet creation | **Not here** — Epic 1 only |

---

### Epic ownership summary

| Epic | What happens | Ledger Engine | Client |
|---|---|---|---|
| **1. Wallet onboarding** | Signup creates wallet | Stores wallet + program pools | Calls `POST /wallets` |
| **2. Transaction processing** | External event → Earn/Burn/Process | **Rule check + posting** | Sends webhook/Kafka event |
| **3. Tier / ranking** | Level up by balance/rules | Exposes balance API | Owns tier logic |
| **4. Process rules** | Hold, expire, settle logic | Posts result | Owns rule engine |

---

## Who it is for

| Audience | Use case |
|---|---|
| **Retail / airline / telco loyalty teams** | Run a points ledger with audit-grade history |
| **Fintech / wallet platforms** | Issue non-fiat balances (LP, credits, miles) under client control |
| **Enterprise IT** | Self-hosted ledger core with no vendor platform lock-in |
| **Integrators** | Embed Earn / Burn / Process flows into CRM, e-commerce, or campaign systems |

Ledger Engine is **ledger core only**. Identity, KYC, payment rails, campaign UI, and notification systems
stay in the client's existing stack and call this service over API.

---

## Core product idea

### One customer = one wallet

Each end customer maps to **exactly one wallet** in the ledger:

```text
Customer ID (from client CRM / membership system)
 ↓
ledger_account.external_reference = unique customer key
 ↓
One LIABILITY account per customer per currency (e.g. LP)
 = the customer's wallet balance
```

Rules:

- **1 customer → 1 wallet** — enforced by unique `external_reference` on `ledger_account`
- The wallet is a COA account, not a separate product object — simple to query, reconcile, and audit
- Client systems own customer master data; Ledger Engine owns balance truth

Example:

| Customer | `external_reference` | Account type | Currency | Role |
|---|---|---|---|---|
| `CUST-10001` | `wallet:CUST-10001:LP` | `LIABILITY` | `LP` | Customer wallet |
| (system) | `pool:loyalty-expense:LP` | `EXPENSE` | `LP` | Points issuance pool |
| (system) | `pool:loyalty-liability:LP` | `LIABILITY` | `LP` | Outstanding points liability |

---

## COA-driven loyalty accounting

Loyalty points are handled through a standard **Chart of Accounts (COA)** — the same five-type model used in
proper accounting:

| COA type | Typical loyalty role |
|---|---|
| **LIABILITY** | Customer wallet balances (points owed to members) |
| **EXPENSE** | Points issued (campaign cost, promotional earn) |
| **REVENUE** | Points recovered on expiry or breakage |
| **ASSET** | Reserved / held points, merchant settlement buckets |
| **EQUITY** | Opening balance, corporate funding of the program |

Every movement is a **balanced journal posting**: debits and credits must match per currency. This gives
finance and audit teams a familiar model instead of ad-hoc balance columns.

```text
Earn 100 LP (purchase bonus)
 DEBIT pool:loyalty-expense:LP 100
 CREDIT wallet:CUST-10001:LP 100

Burn 50 LP (redemption)
 DEBIT wallet:CUST-10001:LP 50
 CREDIT pool:loyalty-liability:LP 50
```

Points calculation logic (rules, tiers, multipliers) lives in the **client's application layer**. Ledger
Engine records the **outcome** as immutable journal entries — what was earned, burned, held, or expired.

---

## Non-fiat currency: Loyalty Points (LP)

Currency is a **2–4 letter ledger unit**, not limited to fiat:

| Code | Meaning |
|---|---|
| `LP` | Loyalty Points |
| `ML` | Miles |
| `CR` | Store credits |
| `USD` | Fiat (if program also tracks cash legs) |

Each `ledger_account` carries one currency. Multi-program clients create separate account sets per unit
(e.g. `LP` wallet + `ML` wallet for the same customer using distinct `external_reference` values).

Balances are **derived from journal entries**, not edited directly — so LP behaves like a proper liability
instrument with full history.

---

## Three operations (product API contract)

All customer-facing mutations reduce to three operations. Each maps to a balanced `journal_transaction`.

| Operation | Business meaning | Typical posting |
|---|---|---|
| **Earn** | Award points/credits | Credit customer wallet; debit issuance expense |
| **Burn** | Redeem or consume | Debit customer wallet; credit liability pool |
| **Process** | System-driven lifecycle | Hold, release, expire, adjust, transfer, settle |

### Process sub-types

| Sub-type | Purpose |
|---|---|
| **Hold** | Reserve points pending order confirmation |
| **Release** | Return held points to available balance |
| **Expire** | Remove expired points (often to revenue/breakage) |
| **Adjust** | Manual correction with audit trail |
| **Transfer** | Move points between customer wallets |
| **Settle** | Finalize a pending earn or burn |

Every operation follows the same pipeline:

```text
Earn / Burn / Process
 ↓
1. Validate rules + balance
2. Create immutable journal_entry
3. Update balance projection (derived from entries)
4. Publish domain event (roadmap)
```

Shared guarantees:

- **Idempotent** — safe to retry via `idempotencyKey`
- **Append-only** — history is never rewritten
- **Correctable** — mistakes are reversed or adjusted with new transactions, not edits

---

## What the client gets

### Deployable standalone product

| Capability | Included |
|---|---|
| Double-entry ledger core | Yes |
| REST API | Yes |
| PostgreSQL + JPA ddl-auto | Yes |
| Docker / docker-compose | Yes |
| Swagger UI | Yes |
| Health / metrics (Actuator) | Yes |
| Private dependency on vendor platform | **No** |
| Kafka / Redis requirement | **No** |

### Enterprise properties

- **Self-hosted** — data stays in the client's infrastructure
- **Auditable** — every balance change has journal lines with timestamp, reference, and idempotency key
- **Concurrent-safe** — account-level locking on post
- **Policy control** — `allow_negative` per account (e.g. wallets cannot go below zero)
- **Reversal support** — compensating entries without deleting history

---

## Deployment model

```text
┌──────────────────────────────────────────────────────────────────┐
│ Enterprise environment │
│ │
│ ┌─────────────┐ ┌──────────────┐ ┌────────────────────────┐ │
│ │ User / │ │ Tier & │ │ Process rules engine │ │
│ │ Membership │ │ ranking │ │ (hold, expire, settle) │ │
│ │ (Epic 1) │ │ (Epic 3) │ │ (Epic 4) │ │
│ └──────┬──────┘ └──────┬───────┘ └───────────┬────────────┘ │
│ │ signup │ read balance │ decide posting │
│ │ create wallet │ │ │
│ └─────────────────┴───────────────────────┘ │
│ │ POST /transactions │
│ ▼ │
│ ┌─────────────────────────┐ │
│ │ Ledger Engine (Epic 2) │ │
│ │ Earn / Burn / Process │ │
│ └─────────────────────────┘ │
│ │ │
│ ▼ │
│ ledger_account / journal_entry │
└──────────────────────────────────────────────────────────────────┘
```

Quick start:

```bash
docker compose up --build
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

Production: activate the `postgres` profile, point at managed PostgreSQL, run behind the client's API
gateway and identity layer.

---

## Integration pattern

Maps to epics above:

| Step | Epic | Action |
|---|---|---|
| User registers | **1** | Client creates `ledger_account` — `externalReference = wallet:{associatedIdentifier}:LP` |
| User earns points | **2** | Client rules calculate amount → `POST /transactions` (Earn legs) |
| User redeems | **2** | `POST /transactions` (Burn legs); engine rejects if insufficient balance |
| User hits tier threshold | **3** | Client reads balance/entries → updates tier in membership system |
| Order hold / expiry | **4** | Rules engine decides Process type → `POST /transactions` |
| Query wallet | **2** | `GET /accounts/{id}/balance`, `GET /accounts/{id}/entries` |
| Correction | **2** | `POST /transactions/{id}/reversal` |

The client's rules engine decides *how many* points and *which Process*; Ledger Engine records *that it
happened*, exactly once.

---

## MVP vs roadmap

| Feature | Epic | Status |
|---|---|---|
| Wallet entity + onboarding API | 1 | **Shipped** |
| Program pool + clearing pool setup | 1 | **Shipped** |
| Deposits / withdrawals / in-wallet transfer | 5–6 | **Shipped** (core journal path) |
| Movement query + manual settle | 7 | **Partial** |
| Rule check + Earn / Burn posting | 2 | **Shipped** |
| Webhook + Kafka transaction ingestion | 2 | **Shipped** |
| Process rules (ADJUST) | 4 | Partial |
| Process HOLD / EXPIRE / SETTLE | 4 | Planned |
| Virtual accounts | 8 | Planned |
| Recipients + linked bank accounts | 9 | Planned |
| FX + payment methods | 10 | Planned |
| Kafka movement pipeline | 11 | Planned |
| OAuth2 / multi-tenancy module | 12 | Planned |
| Card deposit (GrandPay-style) | 5 | Planned |
| Multi-tenant admin UI | — | Out of scope |

---

## Boundaries (what stays outside)

Ledger Engine **includes** wallet operations, movement workflow, and ledger truth. It does **not** replace:

- Customer identity / KYC provider (integrate at wallet activation — Epic 12)
- Card acquirer or bank settlement rails (integrate at deposit/withdrawal webhooks — Epic 5)
- Marketing campaign management or notification/email
- Loyalty tier / multiplier calculation (client reads balance API — Epic 3)

It **is** the standalone successor to : **system of record for balances, movements, and
why they happened** — on double-entry COA with immutable journal history.

---

## Summary

Ledger Engine is the **standalone wallet + ledger product** — successor to — built for
enterprise deployment without platform dependencies:

1. **Wallet provisioning** — `Wallet` + `ledger_account` per owner/currency; CRM batch import
2. **Movements** — deposit, withdrawal, in-wallet transfer via `ledger_movement` → journal
3. **Loyalty** — Earn / Burn / Process via rules + webhook/Kafka ingestion
4. **Tier & ranking** — client reads balance; engine holds truth
5. **Full parity roadmap** — virtual accounts, recipients, FX, SWIFT, compliance, Kafka pipeline, auth

**Domain in one line:** `Wallet` is the product handle; `LedgerMovement` records the business operation;
`JournalTransaction` + `JournalEntry` are the accounting truth; balance is always derived from entries.

Clients **own their infrastructure** and get **capabilities** on a **proper double-entry
foundation** — one unique customer → one wallet, every change recorded as balanced, idempotent, append-only
journal history.
