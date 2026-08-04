# Ledger Engine

A standalone Java 17/Spring Boot double-entry ledger core for customer wallets and balances. It owns ledger
accounts, immutable journal entries, posting, derived balances, idempotency, and reversals.

This is **ledger core**, not payment rails, identity, compliance UI, notification, tenant, or external
settlement orchestration. It has no Kafka, Redis, private service, or private library dependency.

## Naming conventions

| Layer | Convention | Example |
|---|---|---|
| **PO (JPA entity)** | PascalCase class in `domain/` | `LedgerAccount`, `JournalTransaction`, `JournalEntry` |
| **Database table** | snake_case | `ledger_account`, `journal_transaction`, `journal_entry` |
| **Database column** | snake_case | `external_reference`, `idempotency_key`, `sequence_number` |
| **Java field / getter** | camelCase | `externalReference`, `idempotencyKey`, `sequence` |
| **JSON API** | camelCase | `externalReference`, `idempotencyKey`, `accountId` |

PO classes map 1:1 to Flyway tables. `@Column(name = "...")` is used wherever the Java name differs from the
column (for example `sequence` ↔ `sequence_number`).

## Data model (PO ↔ table ↔ columns)

### `LedgerAccount` → `ledger_account`

Chart-of-accounts bucket. Customer wallet balances are modeled as `LIABILITY` (or `ASSET`) accounts.

| Column | Java field | Type / values |
|---|---|---|
| `id` | `id` | UUID PK |
| `external_reference` | `externalReference` | unique business key (wallet / tenant ref) |
| `name` | `name` | display name |
| `type` | `type` | `ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE` |
| `currency` | `currency` | ISO 4217, 3-letter uppercase |
| `status` | `status` | `ACTIVE`, `FROZEN`, `CLOSED` |
| `allow_negative` | `allowNegative` | posting policy flag |
| `version` | `version` | optimistic lock |
| `created_at` | `createdAt` | timestamp |
| `updated_at` | `updatedAt` | timestamp |

Balance is **not stored** on this row. It is derived from `journal_entry` (`debitTotal`, `creditTotal`, signed
`balance` in the API).

### `JournalTransaction` → `journal_transaction`

One balanced posting (Earn, Burn, Process, transfer, etc.) identified by idempotency.

| Column | Java field | Type / values |
|---|---|---|
| `id` | `id` | UUID PK |
| `idempotency_key` | `idempotencyKey` | unique; required for idempotent posting |
| `request_hash` | `requestHash` | SHA-256 of canonical payload |
| `reference` | `reference` | optional business reference (order id, campaign id, …) |
| `description` | `description` | optional narrative |
| `status` | `status` | `POSTED`, `REVERSED` |
| `effective_at` | `effectiveAt` | business effective time |
| `created_at` | `createdAt` | insert time |
| `reversal_of_id` | `reversalOf` | FK → original transaction when this row is a reversal |

### `JournalEntry` → `journal_entry`

Immutable debit/credit line. Append-only; never updated in place.

| Column | Java field | Type / values |
|---|---|---|
| `id` | `id` | UUID PK |
| `transaction_id` | `transaction` | FK → `journal_transaction` |
| `account_id` | `account` | FK → `ledger_account` |
| `side` | `side` | `DEBIT`, `CREDIT` |
| `amount` | `amount` | positive `NUMERIC(38,18)` |
| `currency` | `currency` | must match account currency |
| `sequence_number` | `sequence` | unique per transaction |
| `created_at` | `createdAt` | insert time |

Unique: `(transaction_id, sequence_number)`.

## Three core operations

Product-facing mutations map to balanced `journal_transaction` + `journal_entry` rows:

| Operation | Meaning | Ledger mapping (typical) |
|---|---|---|
| **Earn** | Award points/credits | `CREDIT` customer `ledger_account`; offsetting `DEBIT` on pool/revenue account |
| **Burn** | Redeem / consume | `DEBIT` customer account; offsetting `CREDIT` on pool/expense account |
| **Process** | Hold, release, expire, adjust, transfer, settle | multi-leg `journal_entry` set under one `idempotency_key` |

**Process** sub-types (roadmap — not separate tables yet):

- **Hold** / **Release** — separate liability accounts or future `held` balance projection
- **Expire**, **Adjust**, **Transfer**, **Settle** — expressed as additional balanced postings

## Operation pipeline

Target flow for Earn / Burn / Process (steps 3–4 partially implemented today):

```text
Earn / Burn / Process
        ↓
1. Validate rules + balance          → LedgerService (locks ledger_account, checks allow_negative)
2. Create immutable journal_entry    → append-only rows on journal_entry
3. Update balance projection         → derived from entries (available/held split: roadmap)
4. Publish domain event              → roadmap (no outbox in MVP)
```

## Shared operation guarantees

All three operations must:

- Be **idempotent** via `journal_transaction.idempotency_key` (API: `idempotencyKey`); optional
  `journal_transaction.reference` (API: `reference`) for business correlation only
- Create **append-only** `journal_entry` rows
- **Never mutate history** — corrections use a new transaction or `POST /transactions/{id}/reversal`

## Domain guarantees

- Every transaction has at least two positive entries and balances debits and credits independently per currency.
- `journal_entry` rows are append-only. Historical rows are never updated in place.
- Asset and expense balances increase with debits; liability, equity, and revenue balances increase with credits.
- Posting locks all affected `ledger_account` rows in sorted UUID order and applies `allow_negative` policy.
- Entry `currency` must equal `ledger_account.currency`; only `status = ACTIVE` accounts may be posted.
- Same `idempotency_key` + same payload → existing transaction (`200`); same key + different payload → `409`.
- Reversal appends opposite `journal_entry` rows, sets `reversal_of_id`, marks original `REVERSED`.
- Flyway constraints enforce unique keys, positive amounts, sequence uniqueness, and single reversal per txn.

## Run locally

Requirements: Java 17+ and Maven 3.9+.

```bash
mvn spring-boot:run
```

The default profile uses a file-backed H2 database in PostgreSQL compatibility mode under `./data`.
Swagger UI is at <http://localhost:8080/swagger-ui.html>; health is at
<http://localhost:8080/actuator/health>.

Run tests:

```bash
mvn test
```

Run with PostgreSQL:

```bash
docker compose up --build
```

Run with the **event simulator** (see [INTEGRATION.md](INTEGRATION.md)):

```bash
cp .env.example .env
docker compose --profile simulator up --build
```

For a separately managed PostgreSQL instance:

```bash
SPRING_PROFILES_ACTIVE=postgres \
DB_URL=jdbc:postgresql://localhost:5432/ledger \
DB_USERNAME=ledger \
DB_PASSWORD=change-me \
mvn spring-boot:run
```

## API example

Create source, destination, and equity accounts:

```bash
curl -sS -X POST localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"externalReference":"cash-001","name":"Cash","type":"ASSET","currency":"USD","allowNegative":false}'

curl -sS -X POST localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"externalReference":"cash-002","name":"Settlement","type":"ASSET","currency":"USD","allowNegative":false}'

curl -sS -X POST localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"externalReference":"equity-001","name":"Opening Equity","type":"EQUITY","currency":"USD","allowNegative":false}'
```

Use returned IDs to post opening funds (debit cash, credit equity), then transfer:

```bash
curl -i -X POST localhost:8080/api/v1/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "idempotencyKey":"opening-2026-001",
    "reference":"opening-balance",
    "entries":[
      {"accountId":"<cash-id>","side":"DEBIT","amount":100,"currency":"USD"},
      {"accountId":"<equity-id>","side":"CREDIT","amount":100,"currency":"USD"}
    ]
  }'

curl -i -X POST localhost:8080/api/v1/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "idempotencyKey":"transfer-2026-001",
    "reference":"order-123",
    "entries":[
      {"accountId":"<cash-id>","side":"CREDIT","amount":25,"currency":"USD"},
      {"accountId":"<settlement-id>","side":"DEBIT","amount":25,"currency":"USD"}
    ]
  }'
```

Retrying an identical request returns `200`; its initial posting returns `201`. Query and reverse it:

```bash
curl -sS localhost:8080/api/v1/accounts/<cash-id>/balance
curl -sS 'localhost:8080/api/v1/accounts/<cash-id>/entries?page=0&size=20'
curl -sS localhost:8080/api/v1/transactions/<transaction-id>

curl -i -X POST localhost:8080/api/v1/transactions/<transaction-id>/reversal \
  -H 'Content-Type: application/json' \
  -d '{"idempotencyKey":"reversal-2026-001","description":"Customer correction"}'
```

## Boundaries and layout

```text
com.altech.ledger/
├── domain/           PO entities (LedgerAccount, JournalTransaction, JournalEntry)
├── application/      LedgerService — posting, reversal, balance derivation
├── infrastructure/   Spring Data repositories
├── api/              LedgerController, LedgerDtos (request/response records)
└── resources/db/migration/   Flyway schema (V1__init.sql)
```

- **PO layer** lives in `domain/` (same role as `entity/po/` in Quinsic services).
- **DTOs** are Java records in `api/LedgerDtos`; JSON field names match Java camelCase.
- **Balances** are computed from `journal_entry`, not columns on `ledger_account`.
- **MVP excludes** domain events/outbox, `available_balance` / `held_balance` columns, and dedicated Earn/Burn/Process endpoints — those are product mappings on top of `POST /api/v1/transactions`.

The service is the source of truth for `journal_transaction` and `journal_entry`. Payment execution and
orchestration stay outside this boundary.
