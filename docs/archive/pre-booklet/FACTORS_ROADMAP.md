# Factors roadmap (A–E and beyond)

> **Entry:** [START_HERE.md](./START_HERE.md) · **Spec:** [FACTORS.md](./FACTORS.md)

## Shipped

| ID | Item | Status |
|----|------|--------|
| P1/P2 | Leaf ops + FactorSet boolean (any / atLeast / anyGroup) | ✅ |
| **A** | Explain path (`matchedPath` on eligibilityTrace) | ✅ |
| **B** | `not` · `exactly` · `atMost` · `oneOf` | ✅ |
| **C** | `TIERED_RATE` · `TABLE` · `cap` / `floor` / `multiplier` | ✅ |
| **D** | UAF Factor Playbook PPT | ✅ `docs/decks/LedgeRX-Factor-Playbook.pptx` |
| **E** | This roadmap | ✅ |

## Boolean modes (quick)

| match | Meaning |
|-------|---------|
| `all` | AND (default array) |
| `any` | OR / 1 of N |
| `atLeast` + `count` | ≥ N of M |
| `exactly` + `count` | == N of M |
| `atMost` + `count` | ≤ N of M |
| `not` | none of children |
| `oneOf` | exactly one child |
| `anyGroup` / `allGroups` | group OR / AND |

## Equation extras

```json
{ "type": "RATE", "rate": 0.01, "cap": 50, "floor": 1, "multiplier": 2 }

{ "type": "TIERED_RATE", "brackets": [
  { "upTo": 5000, "rate": 0.01 },
  { "upTo": null, "rate": 0.02 }
]}

{ "type": "TABLE", "by": "tier", "map": {
  "GOLD": { "type": "RATE", "rate": 0.02 },
  "DEFAULT": { "type": "RATE", "rate": 0.01 }
}}
```

## Next (not A–E)

| Phase | Item |
|-------|------|
| P5 | Stacking policy (multi-rule sum / best-of) |
| P6 | Named factor packs catalog |
| P7 | Context snapshots (wallet tags, promo window) |
| P8 | Counters / MTD |
| P9 | Rule pack version + effectiveFrom + approval |

## Principles

1. Door = enter · Brain = equation  
2. No SpEL / arbitrary script  
3. Context arrives as metadata / tags — engine does not become customer-360  
4. Stacking is explicit product switch, not silent first-match accident  
