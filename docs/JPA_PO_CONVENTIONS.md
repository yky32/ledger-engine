# JPA PO conventions (ledger-engine)

Aligned with tgt.pgm / profile-service style.

## Rules

1. **No** multi-arg constructors — no-arg + setters (or `@Builder` when needed)
2. **No** `@Table(name=…)` — physical naming
3. **No** `@Column(length=…)` — dialect defaults
4. **Keep** needed annotations: `@Id` + `@Column`, `nullable`, `precision/scale`, `@Enumerated`, uniqueConstraints, indexes
5. **PK** = Snowflake for all entities
6. **Defaults** = `@PrePersist applyDefaults()` null-only; `@Builder.Default` if `@Builder`
7. **JSONB** only when value is **structured JSON** (`Object` + `JsonBinaryType`):
   - `SystemConfiguration.value`
   - `FailedTransactionIngest.rawPayload`
8. **TEXT** for free-form / description-like blobs (movement metadata, rule content strings, …)
9. Soft delete: `isActive` default true **only when null**

## Entity matrix

| PO | PK | Structured JSON | Defaults |
|----|-----|-----------------|----------|
| Wallet | snowflake | — | type / walletType / status |
| Account | snowflake | — | balances / status + Builder.Default |
| LedgerMovement | snowflake | — (TEXT contexts) | mode / type / status / alias |
| LedgerEntry | snowflake | — | affects* |
| DigestionRule | snowflake | — | op / enabled / priority / … |
| IngestPolicy | snowflake | — | door + auto-wallet |
| FailedTransactionIngest | snowflake | rawPayload | status OPEN |
| SystemConfiguration | snowflake | value | tgt.pgm shape |
| Rule / RuleExecution | snowflake | — (TEXT for now) | — |
| FxRate | snowflake | — | — |
