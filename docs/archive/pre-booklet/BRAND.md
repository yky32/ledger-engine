# LedgeRX — product brand

> **Docs entry:** [START_HERE.md](./START_HERE.md) — 唔好從本檔開始亂跳。

| | |
|--|--|
| **Product name** | **LedgeRX** |
| **Tagline (EN)** | Runtime ledger for loyalty & wallet programmes |
| **Tagline (short)** | Earn · burn · books — without redeploy |
| **Engineering module** | `ledger-engine` (GitHub / Maven artifact / K8s service) |
| **Ops console** | **LedgeRX Admin** (`ledger-engine-admin-portal`) |
| **Optional client SDK** | `ledger-engine-sdk` (contract JAR) |

## How to speak

| Context | Prefer |
|---------|--------|
| Sales / client / pitch | **LedgeRX** |
| Architecture / repo / ticket path | `ledger-engine` |
| Admin UI chrome | **LedgeRX** |
| API host / env vars | keep `LEDGER_ENGINE_*` technical names |

## One-liner (credit card / issuer)

> **LedgeRX** is the points system of record: upstream tells us *what happened*; LedgeRX decides *how many points* (Brain) and posts **true double-entry** books against your programme pool.

## Suite map

```text
LedgeRX
├── Core service     ledger-engine          Door · Brain · Books · Audit
├── Admin console    ledger-engine-admin    Simulator · ops · review
└── (optional) SDK   ledger-engine-sdk      Client JAR delivery
```

## Pronunciation / style

- Written: **LedgeRX** (capital L, capital RX)  
- Not: LedgerX, Ledge Rx, ledgerrx  
- Chinese context: 可叫「LedgeRX」或「LedgeRX 積分賬本」

---

*Brand locked for product surface. Technical identifiers stay `ledger-engine` unless a rename epic is opened.*
