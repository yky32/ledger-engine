# LedgeRX — 業務例：點樣射事件（Ingest → Digest → Book）同點開 COA

> **Docs entry:** [START_HERE.md](./START_HERE.md) — 唔好從本檔開始亂跳。

**Audience:** 業務 / ops / 內部 integrator  
**Product:** LedgeRX（standalone · in-cluster）  
**Module:** `ledger-engine`  
**Status:** 反映 main 已 ship 行為  

**產品口語**（對外講呢套）：

| 口語 | Code / API |
|------|------------|
| **Door**（收唔收） | Ingest · `GET/PUT /ingest-policies` |
| **Brain**（符唔符合 + 計幾多分） | Digestion · `/digestion-rules` |
| **Books**（複式過帳） | Movement + DE legs · PROGRAM pool |
| **COA**（開簿科目段） | `/coa-profiles` · 開 wallet 時用 |

> **唔好**叫 Books 做「accounting rules」——會同 Brain 撈亂。  
> COA **唔決定賺幾多分**；只決定 **account fullNumber 點砌**（內部 accounting）。

相關：`INGEST_VS_DIGESTION.md` · `COA_PROFILE.md` · `DOUBLE_ENTRY_EARN.md` · `API_SURFACE.md` · `CLIENT_EARN_WEBHOOK.md`

---

## 0. 一張圖（成單生意）

```text
 ① 配 COA（可選）     ② 配 Door          ③ 配 Brain
    /coa-profiles         /ingest-policies    /digestion-rules
         │                      │                    │
         └──────────────────────┼────────────────────┘
                                │ runtime DB
                                ▼
 Upstream ──POST webhook──▶  Door ──▶ Brain ──▶ Wallet ──▶ Books(DE)
   POS/OMS                   收流量    資格+計分   開/搵簿     過帳
                                │         │          │          │
                                ▼         ▼          ▼          ▼
                            disabled?  skip/trace  HKD+LP    PROGRAM↔會員
                                                   accounts   movement+legs
                                │
                                ▼
                         結果：balance + API result + Review
```

**Dry-run（只練槍、唔落帳）：**  
`POST /integrations/webhooks/transactions/dry-run` → Door 開關 + Brain + **eligibilityTrace**，**唔**開 wallet、**唔** DE。

---

## 1. 事前：點樣開 COA（Books 用嘅科目骨架）

### 1.1 COA 係咩（業務）

| 係 | 唔係 |
|----|------|
| 開 **Account** 時用邊套 **數字段**（entity/type/subType/buffer） | 賺分公式 |
| 內部 accounting / 總帳對賬用 | Product wallet API 回傳嘅 segment 明細 |
| 一個 `coa_profile.code` = 一套方案 | 每個商戶一葉（唔做） |

預設有 **DEFAULT**（lazy seed，等同舊 `CoaCodes`：entity `10` type `20` …）。

### 1.2 開一套自訂 COA（例：產品線 A）

```bash
BASE=http://localhost:8080

# 列出 / 確保 DEFAULT
curl -sS "$BASE/coa-profiles/default" | jq .

# 新建 profile（idempotent 可先 GET ?code=）
curl -sS -X POST "$BASE/coa-profiles" \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "STREAM_A",
    "name": "Product line A",
    "isDefault": false,
    "isEnabled": true,
    "entity": "01",
    "type": "20",
    "subType": "00",
    "buffer": "00",
    "lpCurrency": "LP",
    "poolAllowNegative": true
  }' | jq .
```

| 欄位 | 業務意思（例） |
|------|----------------|
| `code` | 方案代碼，onboard / Door 引用 |
| `entity` | fullNumber **第一段**（你哋自定語義，例如產品線） |
| `type` / `subType` / `buffer` | 其後科目段 |
| `lpCurrency` | 積分幣標籤（通常 LP） |

Admin：`/coa` 用 form 建/改。

### 1.3 COA 幾時用到？

| 時機 | 點 resolve profile |
|------|-------------------|
| **CRM 明確開戶** | `POST /wallets` body `coaProfileCode` |
| **Webhook 自動開戶** | ① `metadata.coaProfileCode` → ② Door `autoWalletCoaProfileCode` → ③ **DEFAULT** |
| **已有 wallet 再 ensure LP** | 用 wallet 上已 stamp 嘅 `coaProfileCode` |

```text
coa_profile ──segments──▶ Account.fullNumber
                ▲
         onboard / auto-wallet
```

**產品 API 唔會**把 entity/type/… 逐段 return 做會員查詢欄（內部 PO 先有）；對外睇 **fullNumber** + 可選 wallet 上嘅 `coaProfileCode`。

---

## 2. 事前：Door（Ingest）

### 2.1 業務問題

> 而家收唔收上游 webhook？未開過戶嘅會員可唔可以自動開 wallet？

### 2.2 配置例

```bash
curl -sS "$BASE/ingest-policies" | jq .

curl -sS -X PUT "$BASE/ingest-policies" \
  -H 'Content-Type: application/json' \
  -d '{
    "isEnabled": true,
    "isAutoCreateWallet": true,
    "autoWalletSettlementCurrency": "HKD",
    "autoWalletEnsureCurrency": "LP",
    "autoWalletNamePrefix": "Member ",
    "autoWalletCoaProfileCode": "STREAM_A"
  }' | jq .
```

| 欄位 | 業務 |
|------|------|
| `isEnabled` | 總掣：false = 全部事件拒收（DISABLED） |
| `isAutoCreateWallet` | 無 wallet 時 lazy 開戶 |
| `autoWalletSettlementCurrency` | 主簿幣（例 HKD） |
| `autoWalletEnsureCurrency` | 額外積分簿（例 LP） |
| `autoWalletCoaProfileCode` | 自動開戶用邊套 COA（可空 = DEFAULT） |

Admin：`/ingest-policies`。

**Door 唔做：** 幣別/MCC/年齡/幾多分——全部 Brain。

---

## 3. 事前：Brain（Digest）

### 3.1 業務問題

> 邊類事件、邊啲條件先有分？點計？

### 3.2 規則例：超市 1%（MCC 5411）

```bash
curl -sS -X POST "$BASE/digestion-rules" \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "GROCERY_1PCT",
    "name": "Grocery 1% back",
    "priority": 10,
    "isEnabled": true,
    "eventType": "PURCHASE",
    "minAmount": 1,
    "eligibleCurrencies": ["HKD"],
    "eligibleMccs": ["5411"],
    "maxAgeDays": 30,
    "pointCurrency": "LP",
    "formula": { "type": "RATE", "rate": 0.01 },
    "processType": "EARN"
  }' | jq .
```

（實際 field 名以 `DIGESTION_RULES.md` / Admin Brain UI 為準；概念如上。）

### 3.3 評估順序（每條 rule）

```text
priority 由細到大
  eventType 相符？
  amount ≥ minAmount？
  currency 在 eligible 內？（空 list = 全部）
  MCC 在 eligible 內？（空 = 全部；有 list 但 event 無 mcc → 唔過）
  事件年齡 ≤ maxAgeDays？
  → 全過 → 跑 formula → points
  → 第一條全過即 win（其餘唔跑）
```

### 3.4 Formula 類型

| type | 例 | 意思 |
|------|-----|------|
| `RATE` | `{"type":"RATE","rate":0.01}` | 金額 × 1% |
| `FIXED` | `{"type":"FIXED","value":100}` | 固定 100 LP |
| `LINEAR` | `rate` + `fixed` | 比例 + 固定 |
| `AMOUNT` | `{"type":"AMOUNT"}` | 按金額本位（見引擎實作） |

Admin：`/digestion-rules`（filters + formula builder + simulator）。

---

## 4. 射入：Transaction come（上游）

### 4.1 Live 過帳

```bash
EVENT_ID="txn-$(date +%s)"
OWNER="01A-DEMO-0001"

curl -sS -X POST "$BASE/integrations/webhooks/transactions" \
  -H 'Content-Type: application/json' \
  -d "{
    \"eventId\": \"$EVENT_ID\",
    \"ownerId\": \"$OWNER\",
    \"eventType\": \"PURCHASE\",
    \"amount\": 500,
    \"currency\": \"HKD\",
    \"occurredAt\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
    \"metadata\": {
      \"mcc\": \"5411\",
      \"merchantName\": \"Demo Supermarket\",
      \"coaProfileCode\": \"STREAM_A\"
    }
  }" | jq .
```

### 4.2 欄位業務義

| 欄位 | 必填 | 用途 |
|------|------|------|
| `eventId` | ✅ | 唯一事件；幂等 / 對帳 |
| `ownerId` | ✅ | 會員身份 = wallet owner |
| `eventType` | ✅ | Brain 配 rule |
| `amount` / `currency` | ✅ | 計分 + 資格 |
| `occurredAt` | 建議 | 年齡閘 |
| `metadata.mcc` | 視 rule | MCC 資格（別名 mccCode / merchantCategoryCode 亦可） |
| `metadata.coaProfileCode` | 可選 | **僅當要自動開戶時** 指定 COA |

### 4.3 Dry-run（建議先打）

```bash
curl -sS -X POST "$BASE/integrations/webhooks/transactions/dry-run" \
  -H 'Content-Type: application/json' \
  -d "{ ... 同上 body ... }" | jq .
```

睇：`matchedRuleCode` · `points` · `eligibilityTrace[]` · `dryRun: true`。

---

## 5. 引擎內部：Ingest → Digest → Book（逐步）

### Step A — Ingest（Door）

```text
GET effective ingest_policy
if !isEnabled → 結束（DISABLED）
else → 去 Brain
```

### Step B — Digest（Brain）

```text
evaluate(event) →
  match?
    YES → points, operation(EARN/BURN), ruleCode, trace
    NO  → SKIP + reason + trace（可入 fail queue）
```

**例（500 HKD · MCC 5411 · GROCERY_1PCT）：**  
points = 500 × 0.01 = **5 LP**。

### Step C — Wallet

```text
find wallet(ownerId)
  存在 → ensure 有 LP account（用 wallet 已 stamp 嘅 COA）
  不存在 →
    Door.autoCreate?
      YES → POST 等價 onboard：
              settlement HKD + ensure LP
              COA = metadata.coaProfileCode
                    ?? Door.autoWalletCoaProfileCode
                    ?? DEFAULT
      NO  → NO_WALLET 失敗
```

**CRM 預先開戶（可選，唔經 webhook）：**

```bash
curl -sS -X POST "$BASE/wallets" \
  -H 'Content-Type: application/json' \
  -d '{
    "ownerId": "01A-DEMO-0001",
    "settlementCurrency": "HKD",
    "name": "Demo member",
    "coaProfileCode": "STREAM_A",
    "accounts": [{ "currency": "LP", "name": "Points" }]
  }' | jq .
```

### Step D — Book（Double-entry · 唔係舊 /rules）

Earn 5 LP：

```text
Movement EARN · 幂等 key 綁 event
  DEBIT   PROGRAM pool LP    5
  CREDIT  Member LP account  5
→ 2 legs · SETTLED
```

Burn 則方向相反。

```text
會員 LP 結餘 +5
PROGRAM 對手盤平衡
ledger_entries / movements 可查
```

---

## 6. 點樣「睇結果」（業務驗收）

| 想知 | 做咩 |
|------|------|
| 今次賺咗未、邊條 rule | Webhook response / dry-run trace |
| 會員而家幾多 LP | `GET /wallets/{ownerId}` |
| 流水 | `GET /wallets/{ownerId}/movements` |
| 複式兩邊 | `GET /integrations/ledger-entries?eventId=` 或 `movementId=` |
| 點解 skip | fail queue · `eligibilityTrace` · Admin Review |
| COA / Door / Brain 配咩 | Admin `/coa` `/ingest-policies` `/digestion-rules` · `/records` |

Admin 一條龍：

```text
/coa → /ingest-policies → /digestion-rules
  → /simulator 或 /transactions-ingest
  → /review → /ledger-entries
```

---

## 7. 完整業務故事（可講會用）

### 背景

- 計劃：HK 零售積分  
- COA：`STREAM_A`，entity 段 `01`  
- Door：開、可自動開戶、自動 COA = STREAM_A  
- Brain：PURCHASE · HKD · MCC 5411 · 1% · 30 日內  

### 會員第一次喺超市消費 500 HKD

1. **POS 射 webhook**（上文章 payload）  
2. **Door** 開 → 放行  
3. **Brain** 中 `GROCERY_1PCT` → **5 LP** · trace 顯示其他 rule 點 skip  
4. 無 wallet → **自動開** HKD + LP，fullNumber 用 **STREAM_A** 段  
5. **Books** DE：PROGRAM ↔ 會員 LP 各 5  
6. App/CRM 查 `GET /wallets/01A-DEMO-0001` → LP **+5**  
7. 同一 `eventId` 再射 → **DUPLICATE**，唔雙重入帳  

### 反例

| 情況 | 結果 |
|------|------|
| Door `isEnabled=false` | DISABLED，唔入 Brain |
| MCC 5812 但 rule 只得 5411 | SKIP MCC · 無過帳 |
| 事件 60 日前 · maxAge 30 | SKIP AGE |
| 無 wallet · autoCreate false | NO_WALLET |
| currency USD · rule 只 HKD | SKIP CURRENCY |

---

## 8. 責任切表（開會用）

| 問題 | 邊層 |
|------|------|
| 系統仲收唔收單？ | **Door** |
| 未登記會員自動開戶？ | **Door** |
| 用邊套科目開簿？ | **COA**（onboard / auto） |
| 呢單應唔應有分？ | **Brain** filters |
| 有幾多分？ | **Brain** formula |
| 錢/分點入帳平衡？ | **Books** DE |
| 點解今次 0 分？ | dry-run **trace** / fail queue |

---

## 9. 命名提醒（避免講錯）

| ✅ 講 | ❌ 唔好講 |
|------|-----------|
| Door / Brain / Books | 「ingest 負責計分」 |
| Digestion = Brain 資格+公式 | 「accounting rules 計分」 |
| COA profile 開簿 | 「COA 決定 1% 定 2%」 |
| Movement + legs | 舊 `/rules` catalog（已 deprecate） |

---

## 10. 快速 checklist（第一次 demo）

- [ ] Engine up · DB（本地可 `ddl=create`）  
- [ ] `GET /coa-profiles/default`（或自建 STREAM_A）  
- [ ] Door enabled + auto wallet（+ 可選 auto COA）  
- [ ] 至少 1 條 Brain rule（eventType + formula）  
- [ ] dry-run 見到 matchedRule + points + trace  
- [ ] live webhook → `GET /wallets/{ownerId}` LP 有數  
- [ ] legs / movements 對到 eventId  

Bootstrap 捷徑（若有 script）：`./scripts/bootstrap-runtime.sh` 再按本文件改 rule/COA。

---

**文件結束。** 實作以 main API 為準；Admin 路徑見上。更新引擎行為時請同步本頁同 `SYSTEM_BUSINESS_FLOW.md`。
