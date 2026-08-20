# LedgeRX — START HERE

→ **[BOOKLET.md](./BOOKLET.md)**（唯一正文）

| | |
|--|--|
| **Product** | LedgeRX |
| **Code** | `ledger-engine` |
| **Deploy** | In-cluster only |

```text
Event → Door → Brain → ApplyPostingUseCase → Books
```

| | |
|--|--|
| Full doc | [BOOKLET.md](./BOOKLET.md) |
| Decks | [decks/](./decks/) |

```bash
mvn spring-boot:run
./scripts/bootstrap-runtime.sh   # if present
```

Admin: `LEDGER_ENGINE_URL=http://localhost:8080`
