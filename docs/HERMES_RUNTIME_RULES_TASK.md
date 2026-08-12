## Hermes Task: Ledger Engine — Runtime Configurable Earn Rules + Double-entry Visibility

> **Status: DONE** (main, post PR #7–#16)  
> Original brief kept below for history. Implementation notes supersede YAML-era wording.

### Implementation (current)

| Area | How it works now |
|------|------------------|
| Rules | DB `digestion_rule` · API `/digestion-rules` · **no YAML rule catalog / startup seed** |
| Engine | `TransactionRuleEngine` reads **DB only** (empty ⇒ `NO_RULE`) |
| Formula | `AMOUNT` · `RATE:{n}` · `FIXED:{n}` · `MUL_ADD:{rate}:{fixed}` |
| Door / auto-wallet | DB `ingest_policy` · `GET/PUT /ingest-policy` |
| Double-entry | PROGRAM pool legs · earn response `legs` · `GET /integrations/ledger-entries?movementId=` / `?eventId=` |
| Bootstrap | `./scripts/bootstrap-runtime.sh` (API seed) · docs/BOOTSTRAP.md |
| Packages | `ingest` (door) vs `digestion` (brain) |

Docs: [DIGESTION_RULES.md](./DIGESTION_RULES.md) · [DOUBLE_ENTRY_EARN.md](./DOUBLE_ENTRY_EARN.md) · [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) · [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md)

---

### Original context (historical)

Repo: `yky32/ledger-engine`  
Core flow (at task write-time):
- 1 Customer = 1 Wallet
- Webhook `POST /integrations/webhooks/transactions`
- ~~Filter + formula via YAML `ledger.integration.rules`~~ → **replaced by digestion_rule DB**
- Earn points into LP account
- Idempotent by `eventId`
- Failures stored in `failed_transaction_ingest`

### Product goal (must implement) — ✅

客完成交易 → webhook → **runtime configurable filter** → **runtime configurable scoring formula** → deposit LP → **can show double-entry legs**

Rules / formulas changeable **without redeploy / restart**.

---

### Gaps (original) — closed

1. ~~Filter + Formula YAML-only~~ → **DB + API**
2. ~~Formula too limited~~ → + `MUL_ADD`
3. ~~Double-entry visibility~~ → PROGRAM legs + query

---

### Requirements A–D — ✅

#### A. Runtime-configurable rules (no deploy) ✅
`digestion_rule` fields: eventType, operation, enabled, minAmount, eligibleCurrencies, maxAgeDays, pointCurrency, formula, priority.  
APIs: create/update/enable/disable/list/get. Engine reads DB only — **no YAML seed**.

#### B. Configurable scoring ✅
`AMOUNT` · `RATE:{n}` · `FIXED:{n}` · `MUL_ADD:{rate}:{fixed}`

#### C. Earn flow ✅
Webhook: validate → evaluate digestion → filter → points → auto-wallet (ingest policy) → DE earn · fail table on skip · idempotent eventId

#### D. Double-entry ✅
Legs on earn response + `GET /integrations/ledger-entries?movementId=` / `?eventId=`

---

### Non-goals (unchanged)
- Tier / ranking · Campaign UI · Complex CEL · Multi-tenant redesign · 1:1 wallet model change

---

### Acceptance criteria — ✅

1. Runtime rule change without restart  
2. `amount * multiplier` at runtime (`RATE` / `MUL_ADD`)  
3. Idempotent `eventId`  
4. Credit customer LP  
5. Query debit + credit legs  
6. YAML bootstrap **cleanly removed**; API/`bootstrap-runtime.sh` seed  
7. ITs: runtime rule, filter reject, formula, idempotency, balanced legs  

---

### Ops cheat sheet

```bash
./scripts/bootstrap-runtime.sh
curl -sS -X POST 'http://localhost:8080/integrations/webhooks/transactions' \
  -H 'Content-Type: application/json' \
  -d '{"eventId":"txn-1","associatedIdentifier":"01A12345678","eventType":"PURCHASE","amount":200,"currency":"HKD","occurredAt":"2026-08-12T10:00:00Z"}'
curl -sS 'http://localhost:8080/integrations/ledger-entries?eventId=txn-1'
```
