# Ledger Engine — Product Overview

**Ledger Engine** is a standalone, deployable ledger product for enterprise clients who want to run their own
points, credits, or internal-value ledger — without building accounting infrastructure from scratch.

Clients deploy it in their own environment (on-prem, private cloud, or VPC), define their Chart of Accounts
(COA), and integrate via a stable HTTP API. The engine enforces double-entry rules, immutable history, and
idempotent posting so downstream systems can treat it as the financial source of truth.

For technical setup and API details, see [README.md](README.md).

---

## Enterprise product epics

When an enterprise adopts Ledger Engine, they are not just deploying a database — they are standing up a
**loyalty ledger program**. The work breaks into four epics. **Epic 1 is foundational**: nothing else works
without a wallet per user.

```text
Epic 1  User Creation ──▶ Wallet Provisioning     (foundation)
              │
              ▼
Epic 2  Earn / Burn / Process on wallet balance   (ledger core — shipped)
              │
              ├─▶ Epic 3  Tier & ranking            (client rules — reads balance)
              │
              └─▶ Epic 4  Process rules engine      (client pluggable — hold/expire/settle)
```

### Epic 1 — User creation → wallet provisioning ⭐

**The first thing every enterprise must build.**

When a user registers in the client's membership / CRM / app, the client provisions **one wallet** in Ledger
Engine to store that user's points balance.

```text
User signs up (client identity system)
        ↓
Client calls POST /api/v1/accounts
        ↓
ledger_account created
  external_reference = wallet:{userId}:LP
  type               = LIABILITY
  currency           = LP
        ↓
User now has a wallet — balance starts at 0
```

| Responsibility | Owner |
|---|---|
| User profile, login, KYC | Client identity / membership system |
| **Create wallet account per user** | Client integration (triggered on signup) |
| Store points balance truth | **Ledger Engine** |
| Enforce 1 user = 1 wallet | Unique `external_reference` convention |

This epic is **mandatory** for every deployment. Without it, Earn and Burn have nowhere to post.

**Today:** wallet = `ledger_account` row (no separate Wallet API yet — convention via `externalReference`).

---

### Epic 2 — Points balance lifecycle (Earn / Burn / Process)

Once a wallet exists, all point movements go through three operations:

| Operation | Effect on wallet balance | Example |
|---|---|---|
| **Earn** | Add points | Purchase bonus, signup reward, campaign grant |
| **Burn** | Deduct points | Redemption, voucher exchange |
| **Process** | Add, deduct, hold, or move | Expiry, adjustment, transfer, settlement |

```text
                    ┌─────────────────┐
  Earn  ──────────▶ │                 │
  Burn  ──────────▶ │  User wallet    │ ◀── balance derived from journal_entry
  Process ────────▶ │  (LP balance)   │
                    └─────────────────┘
```

Each operation:

1. Validates balance / policy (`allow_negative`)
2. Writes immutable `journal_entry` rows (append-only)
3. Updates derived balance (never mutates history)

**Ledger Engine owns this epic.** Shipped in MVP via `POST /api/v1/transactions`.

---

### Epic 3 — Tier & ranking

When a user reaches a certain balance or lifetime earn threshold, the client promotes them to a higher
**tier** (Silver → Gold → Platinum).

```text
Ledger Engine                    Client tier service
     │                                  │
     │  GET /accounts/{id}/balance      │
     │  GET /accounts/{id}/entries      │
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

**Process** covers lifecycle actions that are not simple earn or burn. Each enterprise configures different
rules:

| Process type | Rule examples |
|---|---|
| **Hold** | Reserve points for 24h pending order confirmation |
| **Release** | Un-hold on cancellation |
| **Expire** | Points older than 12 months → revenue account |
| **Adjust** | CS manual correction with approval workflow |
| **Transfer** | Gift points to another user's wallet |
| **Settle** | Finalize a pending earn after return window |

```text
Business event
      ↓
┌─────────────────────┐
│  Rules engine       │  ← client-owned (or pluggable module)
│  (which Process?    │
│   how many points?) │
└─────────┬───────────┘
          ↓
   POST /transactions  →  Ledger Engine records outcome
```

| Responsibility | Owner |
|---|---|
| Rule definitions, triggers, workflows | **Client rules engine** |
| Execute posting, enforce double-entry | **Ledger Engine** |
| Built-in rule DSL inside engine | Roadmap |

Different enterprises plug in different Process engines ( Drools, custom JSON rules, campaign platform ).
Ledger Engine stays the **execution layer** — it posts what the rules engine decides.

---

### Epic ownership summary

| Epic | What happens | Ledger Engine | Client |
|---|---|---|---|
| **1. User → wallet** | Signup creates wallet account | Stores balance | Triggers account creation |
| **2. Earn / Burn / Process** | Points in/out of wallet | **Core product** | Sends posting requests |
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
ledger_account.external_reference  =  unique customer key
        ↓
One LIABILITY account per customer per currency (e.g. LP)
        =  the customer's wallet balance
```

Rules:

- **1 customer → 1 wallet** — enforced by unique `external_reference` on `ledger_account`
- The wallet is a COA account, not a separate product object — simple to query, reconcile, and audit
- Client systems own customer master data; Ledger Engine owns balance truth

Example:

| Customer | `external_reference` | Account type | Currency | Role |
|---|---|---|---|---|
| `CUST-10001` | `wallet:CUST-10001:LP` | `LIABILITY` | `LP` | Customer wallet |
| (platform) | `pool:loyalty-issuance:LP` | `EXPENSE` | `LP` | Points issuance pool |
| (platform) | `pool:loyalty-liability:LP` | `LIABILITY` | `LP` | Outstanding points liability |

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
  DEBIT   pool:loyalty-expense:LP        100
  CREDIT  wallet:CUST-10001:LP          100

Burn 50 LP (redemption)
  DEBIT   wallet:CUST-10001:LP           50
  CREDIT  pool:loyalty-liability:LP      50
```

Points calculation logic (rules, tiers, multipliers) lives in the **client's application layer**. Ledger
Engine records the **outcome** as immutable journal entries — what was earned, burned, held, or expired.

---

## Non-fiat currency: Loyalty Points (LP)

Currency is a **3-letter ledger unit**, not limited to fiat:

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
| REST API (`/api/v1`) | Yes |
| PostgreSQL + Flyway schema | Yes |
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
│  Enterprise environment                                          │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │ User /      │  │ Tier &       │  │ Process rules engine   │  │
│  │ Membership  │  │ ranking      │  │ (hold, expire, settle) │  │
│  │ (Epic 1)    │  │ (Epic 3)     │  │ (Epic 4)               │  │
│  └──────┬──────┘  └──────┬───────┘  └───────────┬────────────┘  │
│         │ signup          │ read balance          │ decide posting │
│         │ create wallet   │                       │                │
│         └─────────────────┴───────────────────────┘                │
│                              │ POST /transactions                  │
│                              ▼                                     │
│                   ┌─────────────────────────┐                      │
│                   │  Ledger Engine (Epic 2) │                      │
│                   │  Earn / Burn / Process  │                      │
│                   └─────────────────────────┘                      │
│                              │                                     │
│                              ▼                                     │
│                   ledger_account / journal_entry                   │
└──────────────────────────────────────────────────────────────────┘
```

Quick start:

```bash
docker compose up --build
# API: http://localhost:8080/api/v1
# Swagger: http://localhost:8080/swagger-ui.html
```

Production: activate the `postgres` profile, point at managed PostgreSQL, run behind the client's API
gateway and identity layer.

---

## Integration pattern

Maps to epics above:

| Step | Epic | Action |
|---|---|---|
| User registers | **1** | Client creates `ledger_account` — `externalReference = wallet:{userId}:LP` |
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
| Wallet account per user (`external_reference`) | 1 | **Shipped** (convention) |
| First-class `Wallet` API on user signup | 1 | Roadmap |
| COA accounts + double-entry posting | 2 | **Shipped** |
| Earn / Burn via `POST /transactions` | 2 | **Shipped** |
| Idempotency + reversal | 2 | **Shipped** |
| Derived balance API | 2 | **Shipped** |
| Non-fiat currency (`LP`, etc.) | 2 | **Shipped** |
| Dedicated Earn / Burn / Process endpoints | 2 | Roadmap |
| `available_balance` / `held_balance` split | 2 / 4 | Roadmap |
| Tier / ranking storage | 3 | Client-owned |
| Process rules DSL inside engine | 4 | Roadmap (client plugs engine today) |
| Domain event outbox | 2 | Roadmap |
| OAuth2 / API gateway auth | — | Client-side |
| Multi-tenant admin UI | — | Out of scope |

---

## Boundaries (what this product is not)

Ledger Engine does **not** replace:

- Customer identity or authentication
- Loyalty rule / tier / multiplier calculation
- Payment processing or bank settlement
- Marketing campaign management
- Notification or email

It **is** the system of record for **how many points exist, where they moved, and why** — backed by COA and
immutable journal entries.

---

## Summary

Ledger Engine is the **deployable ledger core** in a four-epic enterprise loyalty program:

1. **User creation → wallet provisioning** — every user gets one wallet account to hold LP balance
2. **Earn / Burn / Process** — immutable postings that add or deduct points
3. **Tier & ranking** — client reads balance; engine holds the truth
4. **Process rules engine** — client-defined hold, expire, settle logic; engine executes postings

Clients **build their own ledger** on infrastructure they control, using a **COA model** for loyalty
accounting. Points are a **non-fiat currency** (`LP`), with **one unique customer → one unique wallet** —
and every balance change recorded as balanced, idempotent, append-only journal history.
