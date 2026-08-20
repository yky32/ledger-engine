# LedgeRX — START HERE

**唯一文件入口 → [BOOKLET.md](./BOOKLET.md)**（product + engineering 合一）

| | |
|--|--|
| **Product** | LedgeRX |
| **Code** | `ledger-engine` |
| **Deploy** | In-cluster only |

```text
Event → Door → Brain → ApplyPostingUseCase → Books
```

### 去邊

| 需要 | |
|------|--|
| **全部說明** | **[BOOKLET.md](./BOOKLET.md)** |
| **CTO decks** | [decks/](./decks/) |
| **舊拆檔（archived）** | [archive/](./archive/) |

### 30 秒本地

```bash
mvn spring-boot:run
./scripts/bootstrap-runtime.sh   # if present
./scripts/e2e-smoke.sh           # if present
```

Admin: `LEDGER_ENGINE_URL=http://localhost:8080`
