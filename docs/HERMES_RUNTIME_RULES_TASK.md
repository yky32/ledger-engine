## Hermes Task: Ledger Engine — Runtime Configurable Earn Rules + Double-entry Visibility

### Context
Repo: `yky32/ledger-engine`  
Core flow already works:
- 1 Customer = 1 Wallet
- Webhook `POST /integrations/webhooks/transactions`
- Filter + formula via **YAML** `ledger.integration.rules`
- Earn points into LP account
- Idempotent by `eventId`
- Failures stored in `failed_transaction_ingest`

### Product goal (must implement)
客完成交易 → webhook → **runtime configurable filter** → **runtime configurable scoring formula** → deposit LP → **can show double-entry legs**

Rules / formulas must be changeable **without redeploy / restart**.

---

### Current gaps to close

1. **Filter + Formula are YAML-only**  
   `TransactionRuleEngine` reads `IntegrationProperties` (application.yml).  
   Changing minAmount / currencies / formula requires config change + restart.  
   **Not acceptable.**

2. **Formula too limited**  
   Only supports:
   - `AMOUNT`
   - `RATE:{n}`
   - `FIXED:{n}`  
   Need composable equation with multiple factors.

3. **Double-entry visibility**  
   Earn path posts movement/entries, but response/query must clearly expose debit + credit legs.

Note: There is already a DB `Rule` entity + `/rules` API, but it is **not wired** into the webhook earn flow. Prefer reusing/extending existing structures where sensible, or introduce a proper integration-rule store if cleaner.

---

### Requirements to implement

#### A. Runtime-configurable Integration Rules (no deploy)
Store earn/burn rules in DB (or equivalent runtime store), editable via API.

Each rule should support at least:
- `eventType`
- `operation` (`EARN` / `BURN` / `PROCESS`)
- `enabled` (true/false)
- `minAmount`
- `eligibleCurrencies` (list)
- `maxAgeDays` (nullable)
- `pointCurrency` (default `LP`)
- `formula` / equation definition
- priority / ordering if multiple rules can match

**APIs needed:**
- Create / Update / Enable-Disable rule
- List / Get active rules
- Changes must take effect without application restart

`TransactionRuleEngine` must read from this runtime source (not only YAML).  
YAML may remain as bootstrap/default seed, but runtime DB/API wins.

#### B. Configurable scoring equation
Support composable formula, not only single RATE/FIXED.

Minimum viable:
- `AMOUNT`
- `RATE:{n}` → `amount * n`
- `FIXED:{n}`
- Simple combination, e.g. `amount * rate + fixed`

Design formula storage so more factors can be added later (category, tier, channel, etc.) without schema rewrite if possible.

#### C. Earn flow (keep existing behavior)
Webhook still:
1. Validate payload
2. Evaluate rules (runtime)
3. Filter
4. Compute points
5. Ensure wallet (existing auto-create behavior OK)
6. Post earn to LP account (idempotent by eventId/movementKey)
7. On skip/fail → write `failed_transaction_ingest` + return SKIPPED

#### D. Show double-entry
After successful earn:
- Persist full double-entry (debit + credit)
- API must be able to return:
  - movement id / eventId
  - points earned
  - debit leg (account, amount, currency)
  - credit leg (account, amount, currency)

Either:
- include legs in earn response, and/or
- provide clear query by movementId / eventId that returns legs

---

### Non-goals (do not expand scope)
- Tier / ranking engine
- Campaign UI
- Complex expression language beyond practical composable factors
- Multi-tenant redesign
- Changing 1 Customer = 1 Wallet model

---

### Acceptance criteria
1. Can create/update an earn rule via API and have next webhook use new filter/formula **without restart**
2. Can set formula equivalent to `points = amount * multiplier` and change multiplier at runtime
3. Webhook earn still idempotent on `eventId`
4. Successful earn credits customer LP account
5. Can query/show debit + credit legs for that earn
6. Existing YAML bootstrap still works or is cleanly migrated/seeded
7. Tests cover:
   - runtime rule change affects next event
   - filter reject paths
   - formula calculation
   - idempotent replay
   - double-entry legs present and balanced

---

### Implementation guidance
- Inspect existing:
  - `TransactionRuleEngine`
  - `IntegrationProperties`
  - `IngestTransactionUseCase`
  - `LedgerMovementShooter` / movement + entry model
  - DB `Rule` entity + `/rules` endpoints
- Prefer minimal clean design over big rewrite
- Keep package conventions (`usecase`, `endpoint`, `entity/po`, etc.)
- Add migration/changelog if new tables needed
- Update docs: `docs/CLIENT_EARN_WEBHOOK.md` + short note in README/INTEGRATION

---

### Deliverable
1. Code changes implementing A–D
2. Short summary:
   - what was already done
   - what you added
   - how to create/update a rule at runtime
   - how to query double-entry legs
3. Test evidence (unit/integration)

Start by mapping current earn path end-to-end, then implement runtime rule source + wire `TransactionRuleEngine` to it.
