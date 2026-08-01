# Ledger Engine

A standalone Java 17/Spring Boot double-entry ledger core for customer wallets and balances. It owns ledger
accounts, immutable journal entries, posting, balances, idempotency, and reversals.

This is **ledger core**, not payment rails, identity, compliance UI, notification, tenant, or external
settlement orchestration. It has no Kafka, Redis, private service, or private library dependency.

## Three core operations

All wallet/ledger mutations go through one of three operations:

| Operation | Meaning | Effect on wallet |
|---|---|---|
| **Earn** | Award points/credits to the customer | `+ available_balance` + create ledger entry (credit) |
| **Burn** | Redeem / consume points or credits | `- available_balance` + create ledger entry (debit) |
| **Process** | Intermediate or system-driven action | Hold → release, expire points, adjustment, transfer, or settle a pending transaction |

**Process** is intentionally flexible. Common sub-types:

- **Hold** — reserve amount (`available` → `held`)
- **Release** — un-hold (`held` → `available`)
- **Expire** — burn expired points
- **Adjust** — manual correction
- **Transfer** — between wallets or accounts
- **Settle** — finalize a pending earn/burn

## Operation pipeline

Every Earn / Burn / Process call follows the same path:

```text
Earn / Burn / Process
        ↓
1. Validate rules + balance
2. Create immutable Ledger Entry
3. Update Wallet balance (available / held)
4. Publish domain event
```

## Shared operation guarantees

All three operations must:

- Be **idempotent** via `idempotency_key` or `reference_id`
- Create an **append-only** ledger entry
- **Never directly mutate history** — corrections use a new entry (or reversal), never rewrite past rows

## Domain guarantees

- Every transaction has at least two positive entries and balances debits and credits independently per currency.
- Journal entries are append-only. Historical rows are never updated in place.
- Asset and expense balances increase with debits; liability, equity, and revenue balances increase with credits.
- Posting locks all affected accounts in sorted UUID order and applies nonnegative policy while locks are held.
- Account currency must equal entry currency, and only active accounts may be posted.
- An idempotency key with the same canonical payload returns the existing transaction; a changed payload returns `409`.
- Reversal appends opposite entries, links back to the original transaction, and cannot be performed twice.
- Database constraints reinforce identifiers, references, positive amounts, sequence uniqueness, and single reversal.

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

- `domain`: JPA-backed ledger aggregates and enums.
- `application`: transactional posting, reversal, locking, balance, and policy logic.
- `infrastructure`: Spring Data repositories.
- `api`: versioned HTTP resources, request/response records, validation, and consistent errors.
- `db/migration`: Flyway-owned PostgreSQL-compatible schema.

The MVP deliberately excludes event publishing and outbox infrastructure. Downstream systems should treat this
service as the source of truth for ledger postings, while handling payment execution and business orchestration
outside this boundary.
