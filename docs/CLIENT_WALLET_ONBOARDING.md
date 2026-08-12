# Client wallet onboarding — sample API

**Audience:** integrator / client system adopting Ledger Engine  
**Model:** `1 CUST_ID → 1 Wallet` · `settlementCurrency` = default settlement · multi-currency **accounts under the same wallet**

For earn after onboard: [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md).  
Optional: skip explicit create if `ingest-policy.isAutoCreateWallet=true` ([INGEST_POLICY.md](./INGEST_POLICY.md)).

---

## Customer id

| Field | Value |
|-------|--------|
| JSON | `ownerId` |
| Stored as | `wallet.ownerId` + `wallet.ownerId` |
| Example format | `01A` + **8 digits** → `01A12345678` |
| Uniqueness | One wallet per id (`WAL0409` if already onboarded) |

---

## Use case (this guide)

| | |
|--|--|
| **CUST_ID** | `01A` + 8 digits (e.g. `01A12345678`) |
| **Settlement currency** | `HKD` (wallet default + primary account) |
| **Accounts** | `HKD` (primary / default) + `LP` (loyalty points book) |

```text
CRM CUST 01A12345678
        │
        ▼
   Wallet (settlementCurrency = HKD)
        ├── Account HKD  ← primary (wallet.accountId)
        └── Account LP   ← loyalty balance
```

---

## 1) Create wallet (single customer)

`POST /wallets`

```bash
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "01A12345678",
    "settlementCurrency": "HKD",
    "name": "Customer 01A12345678",
    "associatedFrom": "CRM",
    "accounts": [
      {
        "currency": "LP",
        "name": "Loyalty points",
        "refCode": "LP"
      }
    ]
  }'
```

### Request fields

| Field | Required | Description |
|-------|----------|-------------|
| `ownerId` | ✅ | Client customer id (`01A` + 8 digits) |
| `settlementCurrency` | ✅ | Wallet default settlement (`HKD`) |
| `name` | optional | Display name |
| `associatedFrom` | optional | Source system (default `CRM`) |
| `accounts` | optional | **Extra** books under the wallet. Primary HKD is always created. |
| `accounts[].currency` | for extras | e.g. `LP` |
| `accounts[].name` | optional | Display label |
| `accounts[].refCode` | optional | Product leaf code (defaults to currency code) |
| `accounts[].allowNegative` | optional | default `false` |

Do **not** pass a second wallet for LP — LP is an **account** under the same wallet.

### Success response (shape)

```json
{
  "code": "SYS0000",
  "data": {
    "walletId": 1,
    "ownerId": "01A12345678",
    "ownerId": "01A12345678",
    "associatedFrom": "CRM",
    "settlementCurrency": "HKD",
    "status": "ACTIVE",
    "account": {
      "currency": "HKD",
      "primary": true,
      "ledgerBalance": 0,
      "availableBalance": 0
    },
    "balance": {
      "currency": "HKD",
      "ledgerBalance": 0,
      "availableBalance": 0
    },
    "accounts": [
      {
        "currency": "HKD",
        "primary": true,
        "name": "Customer 01A12345678"
      },
      {
        "currency": "LP",
        "primary": false,
        "refCode": "LP",
        "name": "Loyalty points"
      }
    ]
  }
}
```

### Errors

| HTTP | Code | When |
|------|------|------|
| 409 | `WAL0409` | Same `ownerId` already has a wallet |
| 400 | validation | Missing `ownerId` / `settlementCurrency` |

---

## 2) Query wallet by CUST_ID (`ownerId`)

Client uses **the same id** passed at create.  
Engine returns the **whole structure**: **Wallet → `accounts[]`** (HKD primary + LP, balances, ids).

### Preferred curls

```bash
# Full Wallet : Accounts (all books)
curl -sS 'http://localhost:8080/wallets/01A12345678'
```

```bash
# Same via query param
curl -sS 'http://localhost:8080/wallets?ownerId=01A12345678'
```

```bash
# Filter accounts[] by currency (CSV). Spaces optional.
curl -sS 'http://localhost:8080/wallets/01A12345678?currencies=HKD,LP'
curl -sS 'http://localhost:8080/wallets/01A12345678?currencies=LP'
curl -sS 'http://localhost:8080/wallets?ownerId=01A12345678&currencies=HKD,LP'
```

| Param | Required | Effect |
|-------|----------|--------|
| path / `ownerId` | ✅ | CUST_ID from create |
| `currencies` | optional | CSV filter on **`accounts[]` only** (e.g. `HKD,LP`). Omit = all accounts. |

Wallet header (`walletId`, `ownerId`, `settlementCurrency`, …) always returned.  
`account` / `balance` shortcuts follow the filtered set (prefer primary if still included).

### What you get back (Wallet : Accounts)

```json
{
  "code": "SYS0000",
  "data": {
    "walletId": 1,
    "ownerId": "01A12345678",
    "ownerId": "01A12345678",
    "associatedFrom": "CRM",
    "settlementCurrency": "HKD",
    "status": "ACTIVE",
    "alias": "01A12345678",
    "account": {
      "id": 10,
      "currency": "HKD",
      "primary": true,
      "fullNumber": "…",
      "ledgerBalance": 0,
      "availableBalance": 0
    },
    "balance": {
      "currency": "HKD",
      "ledgerBalance": 0,
      "availableBalance": 0
    },
    "accounts": [
      {
        "id": 10,
        "currency": "HKD",
        "primary": true,
        "name": "Customer 01A12345678",
        "ledgerBalance": 0,
        "availableBalance": 0
      },
      {
        "id": 11,
        "currency": "LP",
        "primary": false,
        "refCode": "LP",
        "name": "Loyalty points",
        "ledgerBalance": 0,
        "availableBalance": 0
      }
    ]
  }
}
```

| Layer | Fields |
|-------|--------|
| **Wallet** | `walletId`, `ownerId`, `settlementCurrency`, `status`, … |
| **Accounts** | `data.accounts[]` — every book under that wallet (HKD + LP) |
| **Primary shortcut** | `data.account` / `data.balance` = settlement (HKD) line |

### Errors

| HTTP | Code | When |
|------|------|------|
| 404 | `WAL0404` | No wallet for this `ownerId` |

---

## 3) Batch onboard (CRM go-live)

`POST /wallets/batch` — max **1000** per request · soft-idempotent

```bash
curl -sS -X POST 'http://localhost:8080/wallets/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "wallets": [
      {
        "ownerId": "01A12345678",
        "settlementCurrency": "HKD",
        "name": "Customer 01A12345678",
        "associatedFrom": "CRM",
        "accounts": [
          { "currency": "LP", "name": "Loyalty points", "refCode": "LP" }
        ]
      },
      {
        "ownerId": "01A87654321",
        "settlementCurrency": "HKD",
        "name": "Customer 01A87654321",
        "associatedFrom": "CRM",
        "accounts": [
          { "currency": "LP", "name": "Loyalty points", "refCode": "LP" }
        ]
      }
    ]
  }'
```

### Batch response

```json
{
  "code": "SYS0000",
  "data": {
    "requested": 2,
    "created": 2,
    "alreadyExists": 0,
    "createdWallets": [ /* … */ ],
    "alreadyExistingAssociatedIdentifiers": []
  }
}
```

Re-run same payload → `created: 0`, `alreadyExists: 2` (no error).

---

## 4) Minimal create (HKD only)

If the customer only needs settlement book (no LP yet):

```bash
curl -sS -X POST 'http://localhost:8080/wallets' \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "01A12345678",
    "settlementCurrency": "HKD",
    "name": "Customer 01A12345678"
  }'
```

---

## Integrator checklist

1. Map CRM customer id → `ownerId` (`01A` + 8 digits).  
2. Call `POST /wallets` (or batch) **before** posting loyalty / cash movements.  
3. Keep **one wallet** per customer; add LP as `accounts[]`, not a second wallet.  
4. Use `GET /wallets/{ownerId}` to verify HKD + LP accounts.  
5. Phase-2 events / deposits reference the same customer id (`ownerId` / `userId` = `01A…`).

---

## Base URL

Replace `http://localhost:8080` with the client environment base URL.  
Envelope: `Result` (`code`, `data`, `requestId`, …). Success code: `SYS0000`.
