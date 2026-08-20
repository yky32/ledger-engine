# LedgeRX — START HERE

**唯一入口。** 其他 `docs/*` 係深讀；日常跟呢一頁就夠。

| | |
|--|--|
| **Product** | LedgeRX |
| **Code** | `ledger-engine` |
| **Deploy** | In-cluster only（API key = 後期 tech debt） |

---

## 30 秒：系統做咩

```text
Upstream 射商業事件
  → Door（收唔收 / 自動開戶）
  → Brain（資格 + 計幾多分）
  → Wallet（1 ownerId → 1 wallet）
  → Books（PROGRAM 複式過帳）
  → 結餘 / movements / legs
```

COA = **開簿科目段**（內部），**唔**決定賺幾多分。

---

## 你係邊個？讀邊份

| 角色 | 讀（由上到下） |
|------|----------------|
| **第一次接觸 / demo** | ① 本頁 → ② [業務射單例](./BUSINESS_SHOOT_EXAMPLE.md) → ③ [BOOTSTRAP](./BOOTSTRAP.md) |
| **產品 / 業務對齊** | ① 本頁 → ② [SYSTEM_BUSINESS_FLOW](./SYSTEM_BUSINESS_FLOW.md) → ③ 射單例 |
| **Upstream 接 API** | ① 本頁 → ② [CLIENT_EARN_WEBHOOK](./CLIENT_EARN_WEBHOOK.md) → ③ [API_SURFACE](./API_SURFACE.md) |
| **開戶 / CRM** | [CLIENT_WALLET_ONBOARDING](./CLIENT_WALLET_ONBOARDING.md) |
| **改 Door / Brain / COA** | [INGEST_VS_DIGESTION](./INGEST_VS_DIGESTION.md) → 各 config 深檔 |
| **工程 / debt** | [TECH_DEBT](./TECH_DEBT.md
- [FACTORS.md
- [FACTORS_ROADMAP.md](./FACTORS_ROADMAP.md) — A–E shipped + next phases](./FACTORS.md) — Door entryFactors + Brain whenFactors (P1/P2)) · [archive/](./archive/) |

---

## 一條 Happy path（本地）

```bash
# 1) 起 engine（DB：本地可用 JPA ddl=create）
# 2) 可選 bootstrap
./scripts/bootstrap-runtime.sh   # 若 script 存在

# 3) 跟業務例打 curl（COA → Door → Brain → dry-run → live → GET wallet）
# 全文：BUSINESS_SHOOT_EXAMPLE.md
```

Admin：`/coa` → `/ingest-policies` → `/digestion-rules` → `/simulator` → `/review`

---

## 文件地圖（精簡）

### A. 必讀（3 份）

| Doc | 內容 |
|------|------|
| **[START_HERE.md](./START_HERE.md)** | 你而家睇緊 |
| **[BUSINESS_SHOOT_EXAMPLE.md](./BUSINESS_SHOOT_EXAMPLE.md)** | 射單：Ingest→Digest→Book + 開 COA · curl · checklist |
| **[SYSTEM_BUSINESS_FLOW.md](./SYSTEM_BUSINESS_FLOW.md)** | 產品全景 · actor · 配置邊界 |
| **[decks/LedgeRX-Product-Overview.pptx](./decks/LedgeRX-Product-Overview.pptx)** | C-level briefing |
| **[decks/LedgeRX-UAFinance-Full-Briefing.pptx](./decks/LedgeRX-UAFinance-Full-Briefing.pptx)** | **Complete** product+arch+landscape+earn/burn+appendix |
| **[decks/LedgeRX-UAFinance-Landscape-Apply.pptx](./decks/LedgeRX-UAFinance-Landscape-Apply.pptx)** | (same content / alias) |
| **[UAF_EVENT_MAPPING_SKELETON.md](./UAF_EVENT_MAPPING_SKELETON.md)** | CC~21 + Loan event → Brain skeleton |
| **[decks/LedgeRX-UAF-Earn-Process-Burn-Sequence.pptx](./decks/LedgeRX-UAF-Earn-Process-Burn-Sequence.pptx)** | SA-style earn→process→burn sequence demo |
| **[decks/uaf-earn-process-burn-sequence.html](./decks/uaf-earn-process-burn-sequence.html)** | Interactive / printable sequence HTML |

### B. 配置深讀（要改規則先開）

| Doc | API |
|------|-----|
| [INGEST_VS_DIGESTION.md](./INGEST_VS_DIGESTION.md) | Door vs Brain 一分鐘 |
| [INGEST_POLICY.md](./INGEST_POLICY.md) | Door 欄位 |
| [DIGESTION_RULES.md](./DIGESTION_RULES.md) | Brain filters + formula |
| [COA_PROFILE.md](./COA_PROFILE.md) | `/coa-profiles` |

### C. 接線 / 查帳

| Doc | |
|------|--|
| [CLIENT_EARN_WEBHOOK.md](./CLIENT_EARN_WEBHOOK.md) | Webhook payload |
| [CLIENT_WALLET_ONBOARDING.md](./CLIENT_WALLET_ONBOARDING.md) | 明確開戶 |
| [DOUBLE_ENTRY_EARN.md](./DOUBLE_ENTRY_EARN.md) | PROGRAM DE |
| [HOLD_RELEASE.md](./HOLD_RELEASE.md) | Hold |
| [HISTORY_ASOF_REPLAY.md](./HISTORY_ASOF_REPLAY.md) | 流水 / as-of / fail |
| [API_SURFACE.md](./API_SURFACE.md) | 產品 API vs legacy |
| [BOOTSTRAP.md](./BOOTSTRAP.md) | 空庫啟動 |
| [BRAND.md](./BRAND.md) | 命名 LedgeRX |

### D. 工程

| Doc | |
|------|--|
| [TECH_DEBT.md](./TECH_DEBT.md) | API key 後期、migration… |
| [archive/](./archive/) | 舊 task、CC 長篇 scenarios、JPA 筆記 — **唔係日常入口** |

### E. Repo root

| Doc | |
|------|--|
| [../README.md](../README.md) | 工程 README |
| [../PRODUCT.md](../PRODUCT.md) | 產品摘要 |
| [../INTEGRATION.md](../INTEGRATION.md) | 集成摘要 |

---

## 名詞（固定口徑）

| 講 | 唔好講 |
|----|--------|
| Door / Brain / Books | 「ingest 計分」「accounting rules 計分」 |
| ownerId | 用 vanity 當身份 |
| COA = 開簿段 | COA = 賺幾多 % |
| `/wallets` `/movements` `/digestion-rules` | 新功能用 deprecated `/ledger-*` `/rules` |

---

## 維護規則（防 docs 再炸）

1. **新同事 / 新 session 只丟 `START_HERE.md` link。**  
2. 大改流程 → 先改 **BUSINESS_SHOOT_EXAMPLE** + **SYSTEM_BUSINESS_FLOW**，再改深檔。  
3. 一次性 task / 過期 brief → 丟 **`docs/archive/`**，唔放 README 主表。  
4. 唔好平行再開第三份「全流程」長文；要加料就 patch 射單例或 business flow。  
5. Admin 文案同呢度 Door/Brain/Books 對齊。

---

**完。** 下一步：開 [業務射單例](./BUSINESS_SHOOT_EXAMPLE.md)。
