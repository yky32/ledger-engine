# Ingest policy — what it is (example)

**Product name:** ingest policy  
**Table:** `ingest_policy`  
**API:** `GET/PUT /ingest-policy`  
**Role:** 控制 **webhook 入嚟之後** 點做 — 開唔開、冇 wallet 自唔自動開。

唔係 digestion formula（嗰個係 `/digestion-rules`）。

---

## 一個 row 長點樣（DB / JSON）

```json
{
  "id": 345812345678901234,
  "isEnabled": true,
  "isAutoCreateWallet": true,
  "autoWalletSettlementCurrency": "HKD",
  "autoWalletEnsureCurrency": "LP",
  "autoWalletAssociatedFrom": "CRM",
  "autoWalletNamePrefix": "Auto "
}
```

| Field | 例子意思 |
|-------|----------|
| `isEnabled=true` | 收 webhook；`false` → 全部 SKIPPED `DISABLED` |
| `isAutoCreateWallet=true` | gates 過咗、客未有 wallet → 自動開 |
| `autoWalletSettlementCurrency=HKD` | 自動開時 primary = HKD |
| `autoWalletEnsureCurrency=LP` | 同時開 LP book（earn 落呢度） |
| `autoWalletAssociatedFrom=CRM` | wallet.associatedFrom |
| `autoWalletNamePrefix=Auto ` | nickname = `Auto CUST_124` |

通常 **得 1 行 active**（成個 engine 一份政策）。

---

## 流程入邊邊度用

```text
POST /integrations/webhooks/transactions
        │
        ▼
  ingest_policy.isEnabled ?  ──no──► SKIPPED DISABLED
        │ yes
        ▼
  digestion_rule (filter + formula)
        │ match
        ▼
  wallet exists?
    yes → ensure LP book
    no  → ingest_policy.isAutoCreateWallet?
              yes → create wallet (HKD+LP per policy fields)
              no  → NO_WALLET fail
        │
        ▼
  EARN/BURN (PROGRAM double-entry)
```

---

## 實例 A — 預設 adopt（lazy wallet）

```bash
GET /ingest-policy
# → isAutoCreateWallet: true, settlement HKD, ensure LP
```

客 `CUST_124` 第一次 PURCHASE webhook、未 onboard：

1. Digestion：RATE 0.01 → 2 LP  
2. Policy：auto-create → Wallet + HKD + LP  
3. Earn：CREDIT CUST LP / DEBIT PROGRAM  

---

## 實例 B — 關閉 auto-create（要先 onboard）

```bash
PUT /ingest-policy
{ "isAutoCreateWallet": false }
```

未 onboard 嘅 webhook → `NO_WALLET` fail row；已 onboard 照 earn。

---

## 實例 C — 停晒 webhook

```bash
PUT /ingest-policy
{ "isEnabled": false }
```

任何 event → SKIPPED `DISABLED`（唔跑 digestion / 唔開 wallet）。

---

## API

```bash
curl -sS 'http://localhost:8080/ingest-policy'

curl -sS -X PUT 'http://localhost:8080/ingest-policy' \
  -H 'Content-Type: application/json' \
  -d '{
    "isEnabled": true,
    "isAutoCreateWallet": true,
    "autoWalletSettlementCurrency": "HKD",
    "autoWalletEnsureCurrency": "LP",
    "autoWalletAssociatedFrom": "CRM",
    "autoWalletNamePrefix": "Auto "
  }'
```

First GET/PUT（或 first earn）會 materialize 預設 row。  
Env `ledger.integration.*` 只影響 **第一行 seed**，之後以 DB 為準。

---

## 同其他表對照

| 名 | 管咩 |
|----|------|
| **`ingest_policy`** | webhook 總掣 + auto-wallet 點開 |
| **`digestion_rule`** | 邊啲 event / formula / points |
| **`wallet` / `account`** | 客嘅錢同分 |
| **`PROGRAM` wallet** | DE counterparty pool |

Index: [README.md](./README.md)
