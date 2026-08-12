# JPA PO conventions (ledger-engine)

Aligned with tgt.pgm / profile-service style.

## Rules

1. **No** multi-arg constructors — no-arg + setters (or `@Builder` when needed)
2. **No** `@Table(name=…)` — physical naming
3. **No** `@Column(length=…)` — dialect defaults
4. **Keep** needed annotations: `@Id`, `@Column`, `@Column(nullable=false)`, `precision/scale`, `@Enumerated`, uniqueConstraints, indexes
5. **PK** = Snowflake (`@GenericGenerator` + `SnowflakeIdGenerator`) for all entities
6. **Defaults** = `@PrePersist applyDefaults()` null-only; `@Builder.Default` if `@Builder`
7. **JSON** = `@Type(JsonBinaryType.class)` + `columnDefinition = "jsonb"` + `Object` (hypersistence-utils)
8. Soft delete base: `AuditEntityWithIsActive` sets `isActive=true` only when null

## Entities

| PO | PK | JSONB | Defaults |
|----|-----|-------|----------|
| Wallet | snowflake | — | type/walletType/status |
| Account | snowflake | — | balances/status + Builder.Default |
| LedgerMovement | snowflake | event, metadata, contexts, files | mode/type/status/alias |
| LedgerEntry | snowflake | — | affects* |
| DigestionRule | snowflake | — | operation/enabled/priority/… |
| IngestPolicy | snowflake | — | door + auto-wallet |
| FailedTransactionIngest | snowflake | rawPayload | status OPEN |
| SystemConfiguration | snowflake | value | — (tgt.pgm shape) |
| Rule / RuleExecution | snowflake | content / metadata | — |
| FxRate | snowflake | — | — |
