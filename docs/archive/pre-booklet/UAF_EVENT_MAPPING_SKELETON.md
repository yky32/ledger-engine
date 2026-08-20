# UA Finance — Event → Brain mapping skeleton

> **Docs entry:** [START_HERE.md](./START_HERE.md)  
> **Audience:** UAF Internal IT + Biz + LedgeRX  
> **Status:** Skeleton — replace `eventCode` with real e6 / loan catalogue codes.

Related deck: `docs/decks/LedgeRX-UAFinance-Landscape-Apply.pptx`

---

## How to use

1. Export real event codes from **cc-xapi / e6** (~21) and **loan** (N).  
2. For each row: set `mapsToEventType`, `brainRuleCode`, `formulaIntent`, `coaProfile`, `phase`.  
3. Implement Brain rules in Admin / `POST /digestion-rules`.  
4. Prove with **SDK dry-run** before live.  
5. Keep **one LedgeRX** — CC vs Loan via `coaProfileCode` / rule sets, not multiple ledger services.

### Column dictionary

| Column | Meaning |
|--------|---------|
| eventCode | UAF source code (stable) |
| channel | `CC` \| `LOAN` \| `BOTH` |
| mapsToEventType | `TransactionalEvent.eventType` |
| brainRuleCode | `digestion_rule.code` |
| formulaIntent | RATE / FIXED / AMT / NONE / HOLD |
| coaProfile | e.g. `UAF_CC` / `UAF_LOAN` / product codes you define |
| phase | P1 dry-run · P1 live · P2 · OUT |
| status | TODO · MAPPED · UAT · LIVE |
| notes | MCC, min amount, skip reasons |

---

## CC skeleton (~21)

| # | eventCode (placeholder) | channel | mapsToEventType | brainRuleCode | formulaIntent | coaProfile | phase | status | notes |
|---|-------------------------|---------|-----------------|---------------|---------------|------------|-------|--------|-------|
| 01 | CC_PURCHASE_POSTED | CC | PURCHASE | CC_EARN_BASE | RATE | CC | P1 | TODO | core earn |
| 02 | CC_PURCHASE_AUTH | CC | AUTH | CC_HOLD_OPT | HOLD | CC | P2 | TODO | optional hold |
| 03 | CC_PURCHASE_CAPTURE | CC | CAPTURE | CC_EARN_BASE | RATE | CC | P1 | TODO | if separate from posted |
| 04 | CC_REFUND | CC | REFUND | CC_BURN_REFUND | RATE/AMT | CC | P1 | TODO | clawback points |
| 05 | CC_CHARGEBACK | CC | CHARGEBACK | CC_BURN_CB | AMT | CC | P1 | TODO | |
| 06 | CC_FX_MARKUP | CC | PURCHASE | CC_EARN_FX | RATE | CC | P2 | TODO | |
| 07 | CC_CASH_ADVANCE | CC | CASH_ADV | SKIP_OR_RATE | TBD | CC | P2 | TODO | policy |
| 08 | CC_FEE_POSTED | CC | FEE | SKIP | NONE | CC | P1 | TODO | usually no earn |
| 09 | CC_PAYMENT_RECV | CC | PAYMENT | SKIP | NONE | CC | P1 | TODO | |
| 10 | CC_INTEREST_POST | CC | INTEREST | SKIP | NONE | CC | P1 | TODO | |
| 11 | CC_MCC_GROCERY | CC | PURCHASE | CC_EARN_MCC_5411 | RATE+ | CC | P1 | TODO | metadata.mcc |
| 12 | CC_MCC_DINING | CC | PURCHASE | CC_EARN_MCC_5812 | RATE+ | CC | P1 | TODO | |
| 13 | CC_MCC_TRAVEL | CC | PURCHASE | CC_EARN_TRAVEL | RATE+ | CC | P2 | TODO | |
| 14 | CC_ONLINE_ECOM | CC | PURCHASE | CC_EARN_ONLINE | RATE | CC | P1 | TODO | |
| 15 | CC_INSTALMENT | CC | INSTALL | CC_EARN_OR_SKIP | TBD | CC | P2 | TODO | |
| 16 | CC_REWARD_REDEEM | CC | REDEEM | CC_BURN_REDEEM | AMT | CC | P1 | TODO | + coupon out |
| 17 | CC_REWARD_EXPIRE | CC | EXPIRE | CC_BURN_EXPIRE | AMT | CC | P2 | TODO | |
| 18 | CC_ADJUSTMENT_CR | CC | ADJ_CR | CC_EARN_ADJ | AMT | CC | P1 | TODO | ops |
| 19 | CC_ADJUSTMENT_DR | CC | ADJ_DR | CC_BURN_ADJ | AMT | CC | P1 | TODO | ops |
| 20 | CC_CARD_ACTIVATION | CC | SIGNUP | CC_EARN_WELCOME | FIXED | CC | P1 | TODO | welcome LP |
| 21 | CC_ANNUAL_FEE_WAIVE | CC | FEE_WAIVE | SKIP_OR_FIXED | TBD | CC | P2 | TODO | |

> Replace placeholder `eventCode` with **real e6 codes**. If one e6 event covers multiple MCC boosts, use **one eventType PURCHASE** + Brain MCC lists instead of separate event codes.

---

## Loan skeleton (N — start 4+)

| # | eventCode (placeholder) | channel | mapsToEventType | brainRuleCode | formulaIntent | coaProfile | phase | status | notes |
|---|-------------------------|---------|-----------------|---------------|---------------|------------|-------|--------|-------|
| L1 | LOAN_DISBURSE | LOAN | DISBURSE | LN_EARN_OR_SKIP | TBD | LOAN | P1 | TODO | policy |
| L2 | LOAN_REPAYMENT | LOAN | REPAY | LN_EARN_REPAY | RATE/FIXED | LOAN | P1 | TODO | |
| L3 | LOAN_EARLY_SETTLE | LOAN | SETTLE | LN_EARN_BONUS | FIXED | LOAN | P2 | TODO | |
| L4 | LOAN_OVERDUE | LOAN | OVERDUE | SKIP_OR_HOLD | NONE | LOAN | P2 | TODO | |
| L5 | LOAN_… | LOAN | TBD | TBD | TBD | LOAN | TBD | TODO | fill N |

---

## xapi → TransactionalEvent (field map)

| UAF / xapi field | LedgeRX field | Required | Notes |
|------------------|---------------|----------|-------|
| txn id / uuid | `eventId` | ✅ | idempotency |
| customer / member id | `ownerId` | ✅ | wallet key |
| event / msg type | `eventType` | ✅ | after map |
| amount | `amount` | ✅ | |
| currency | `currency` | ✅ | |
| event time | `occurredAt` | ✅ | age gate |
| mcc | `metadata.mcc` | if rules need | |
| product line | `metadata.coaProfileCode` | optional | CC vs LOAN open |
| merchant | `metadata.merchantName` | optional | |

---

## Brain rule backlog (create in Admin)

| code | eventType | intent | priority | phase |
|------|-----------|--------|----------|-------|
| CC_EARN_BASE | PURCHASE | base % | 100 | P1 |
| CC_EARN_MCC_5411 | PURCHASE | grocery boost | 10 | P1 |
| CC_EARN_MCC_5812 | PURCHASE | dining boost | 20 | P1 |
| CC_BURN_REFUND | REFUND | clawback | 10 | P1 |
| CC_BURN_REDEEM | REDEEM | redemption | 10 | P1 |
| CC_EARN_WELCOME | SIGNUP | fixed welcome | 10 | P1 |
| LN_EARN_REPAY | REPAY | loan repay earn | 10 | P1 |

---

## Workshop checklist

- [ ] Paste real 21 CC codes  
- [ ] Paste loan N codes  
- [ ] Agree P1 rows only  
- [ ] ownerId strategy (1 person · CC+Loan books)  
- [ ] COA profile codes created in LedgeRX  
- [ ] 5 golden dry-runs via SDK  
- [ ] Redemption sequence with E-Coupon adapter  

---

*Skeleton only — not production config.*
