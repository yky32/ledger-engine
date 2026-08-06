#!/usr/bin/env python3
"""
Ledger Engine external-system simulator.

Modes:
  backfill  — Phase 1: bulk-create wallets (e.g. 70K UAfinance customers 1:1)
  webhook   — Phase 2: shoot PURCHASE/etc events via HTTP
  kafka     — Phase 2: shoot events via Kafka
  both      — Phase 2 webhook + Kafka

Phase 1 example (simulate UAfinance 70K backfill):
  SIM_MODE=backfill SIM_USER_COUNT=70000 SIM_USER_ID_PREFIX=UAF- SIM_CURRENCY=LP \\
    python simulator.py

Or from CSV (one customer id per line, or header userId/customerId/id):
  SIM_MODE=backfill SIM_CUSTOMER_FILE=/data/uafinance_customers.csv python simulator.py
"""

from __future__ import annotations

import csv
import json
import os
import random
import sys
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

import requests

try:
    from kafka import KafkaProducer
except ImportError:
    KafkaProducer = None


def env(name: str, default: str) -> str:
    return os.getenv(name, default)


def env_int(name: str, default: int) -> int:
    return int(os.getenv(name, str(default)))


def env_float(name: str, default: float) -> float:
    return float(os.getenv(name, str(default)))


def env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.lower() in ("1", "true", "yes", "y", "on")


def ledger_base_url() -> str:
    explicit = env("SIM_LEDGER_BASE_URL", "").strip()
    if explicit:
        return explicit.rstrip("/")
    webhook = env("SIM_WEBHOOK_URL", "http://app:8080/integrations/webhooks/transactions")
    marker = "/integrations/webhooks"
    if marker in webhook:
        return webhook.split(marker, 1)[0].rstrip("/")
    return "http://app:8080"


def wait_for_ledger(timeout_sec: float = 120.0) -> None:
    """Block until ledger health is up (useful in docker compose)."""
    base = ledger_base_url()
    url = f"{base}/actuator/health"
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        try:
            r = requests.get(url, timeout=3)
            if r.status_code == 200:
                print(f"[health] ledger ready at {url}")
                return
        except requests.RequestException:
            pass
        time.sleep(2)
    print(f"[health] WARN: ledger not healthy after {timeout_sec}s — continuing anyway", file=sys.stderr)


def load_customer_ids() -> list[str]:
    """
    Load customer ids for backfill / event rotation.
    Priority: SIM_CUSTOMER_FILE > generated SIM_USER_ID_PREFIX + count.
    """
    path = env("SIM_CUSTOMER_FILE", "").strip()
    if path:
        return load_customer_file(path)

    user_prefix = env("SIM_USER_ID_PREFIX", "UAF-")
    user_count = env_int("SIM_USER_COUNT", 5)
    width = max(5, len(str(user_count)))
    return [f"{user_prefix}{i:0{width}d}" for i in range(1, user_count + 1)]


def load_customer_file(path: str) -> list[str]:
    p = Path(path)
    if not p.is_file():
        raise FileNotFoundError(f"SIM_CUSTOMER_FILE not found: {path}")

    ids: list[str] = []
    text = p.read_text(encoding="utf-8").strip()
    if not text:
        return ids

    # JSON array of strings or objects
    if text.startswith("["):
        data = json.loads(text)
        for row in data:
            if isinstance(row, str):
                ids.append(row.strip())
            elif isinstance(row, dict):
                uid = row.get("userId") or row.get("customerId") or row.get("id") or row.get("memberId")
                if uid:
                    ids.append(str(uid).strip())
        return [x for x in ids if x]

    # CSV or plain lines
    with p.open(newline="", encoding="utf-8") as fh:
        sample = fh.read(4096)
        fh.seek(0)
        if "," in sample.splitlines()[0]:
            reader = csv.DictReader(fh)
            fields = [f.lower() for f in (reader.fieldnames or [])]
            key = None
            for candidate in ("userid", "customerid", "id", "memberid", "customer_id", "user_id"):
                if candidate in fields:
                    key = reader.fieldnames[fields.index(candidate)]
                    break
            if key is None and reader.fieldnames:
                key = reader.fieldnames[0]
            for row in reader:
                val = (row.get(key) or "").strip()
                if val:
                    ids.append(val)
        else:
            for line in fh:
                val = line.strip()
                if val and not val.startswith("#"):
                    ids.append(val)
    return ids


def chunked(items: list[str], size: int) -> Iterable[list[str]]:
    for i in range(0, len(items), size):
        yield items[i : i + size]


def backfill_wallets(users: list[str], currency: str) -> dict:
    """
    Phase 1 CRM backfill via POST /wallets/batch (max 1000 per request by API).
    Idempotent: re-runs count alreadyExists.
    """
    base = ledger_base_url()
    batch_size = min(env_int("SIM_BATCH_SIZE", 500), 1000)  # API max = 1000
    timeout = env_float("SIM_HTTP_TIMEOUT", 120.0)
    pause = env_float("SIM_BATCH_PAUSE_SECONDS", 0.05)
    url = f"{base}/wallets/batch"

    total = len(users)
    created = 0
    already = 0
    failed = 0
    t0 = time.time()

    print(
        f"[backfill] start customers={total} currency={currency} "
        f"batch_size={batch_size} url={url}"
    )

    for batch_idx, batch in enumerate(chunked(users, batch_size), start=1):
        payload = {
            "wallets": [
                {
                    "userId": uid,
                    "currency": currency,
                    "name": f"Wallet {uid}",
                    "externalId": uid,
                    "externalType": env("SIM_EXTERNAL_TYPE", "uafinance"),
                }
                for uid in batch
            ]
        }
        batch_created = 0
        batch_already = 0
        try:
            response = requests.post(url, json=payload, timeout=timeout)
            if response.status_code >= 400:
                print(
                    f"[backfill] batch {batch_idx} HTTP {response.status_code}: {response.text[:300]}",
                    file=sys.stderr,
                )
                failed += len(batch)
            else:
                body = response.json()
                batch_created = int(body.get("created", 0))
                batch_already = int(body.get("alreadyExists", 0))
                created += batch_created
                already += batch_already
        except requests.RequestException as ex:
            print(f"[backfill] batch {batch_idx} error: {ex}", file=sys.stderr)
            failed += len(batch)

        done = min(batch_idx * batch_size, total)
        elapsed = time.time() - t0
        rate = done / elapsed if elapsed > 0 else 0
        eta = (total - done) / rate if rate > 0 else 0
        print(
            f"[backfill] batch {batch_idx} "
            f"progress={done}/{total} ({100.0 * done / total:.1f}%) "
            f"created+={batch_created} already+={batch_already} "
            f"rate={rate:.0f}/s eta={eta/60:.1f}m"
        )
        if pause > 0:
            time.sleep(pause)

    elapsed = time.time() - t0
    summary = {
        "requested": total,
        "created": created,
        "alreadyExists": already,
        "failed": failed,
        "elapsedSeconds": round(elapsed, 2),
        "walletsPerSecond": round(total / elapsed, 2) if elapsed > 0 else 0,
    }
    print(f"[backfill] DONE {json.dumps(summary)}")
    return summary


def onboard_wallets_one_by_one(users: list[str], currency: str) -> None:
    """Legacy single-create path (small sims only)."""
    base = ledger_base_url()
    for user_id in users:
        url = f"{base}/wallets"
        payload = {"userId": user_id, "currency": currency, "name": f"Sim wallet {user_id}"}
        response = requests.post(url, json=payload, timeout=30)
        if response.status_code in (201, 409):
            print(f"[onboard] {user_id} -> {response.status_code}")
        else:
            response.raise_for_status()


def build_event(user_id: str) -> dict:
    amount_min = env_float("SIM_AMOUNT_MIN", 10.0)
    amount_max = env_float("SIM_AMOUNT_MAX", 500.0)
    amount = round(random.uniform(amount_min, amount_max), 2)
    return {
        "eventId": f"evt-{uuid.uuid4()}",
        "userId": user_id,
        "eventType": env("SIM_EVENT_TYPE", "PURCHASE"),
        "amount": amount,
        "currency": env("SIM_CURRENCY", "LP"),
        "occurredAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "metadata": {"source": "event-simulator"},
    }


def send_webhook(event: dict) -> None:
    url = env("SIM_WEBHOOK_URL", "http://app:8080/integrations/webhooks/transactions")
    response = requests.post(url, json=event, timeout=30)
    response.raise_for_status()
    print(
        f"[webhook] {event['eventId']} user={event['userId']} "
        f"amount={event['amount']} -> {response.json()}"
    )


def send_kafka(producer, topic: str, event: dict) -> None:
    payload = json.dumps(event).encode("utf-8")
    producer.send(topic, key=event["eventId"].encode("utf-8"), value=payload).get(timeout=10)
    print(
        f"[kafka]   {event['eventId']} user={event['userId']} "
        f"amount={event['amount']} -> topic={topic}"
    )


def run_events(users: list[str], mode: str) -> int:
    interval = env_float("SIM_INTERVAL_SECONDS", 2.0)
    max_count = env_int("SIM_TRANSACTION_COUNT", 20)

    producer = None
    topic = env("SIM_KAFKA_TOPIC", "ledger.transaction.events")
    if mode in ("kafka", "both"):
        if KafkaProducer is None:
            print("kafka-python not installed", file=sys.stderr)
            return 1
        bootstrap = env("SIM_KAFKA_BOOTSTRAP", "kafka:9092")
        producer = KafkaProducer(bootstrap_servers=bootstrap.split(","))
        print(f"Kafka producer connected to {bootstrap}, topic={topic}")

    webhook_url = env("SIM_WEBHOOK_URL", "http://app:8080/integrations/webhooks/transactions")
    print(
        f"Simulator mode={mode} interval={interval}s "
        f"count={max_count or 'unlimited'} currency={env('SIM_CURRENCY', 'LP')} users={len(users)}"
    )
    if mode in ("webhook", "both"):
        print(f"Webhook URL={webhook_url}")

    sent = 0
    while max_count == 0 or sent < max_count:
        user_id = random.choice(users)
        event = build_event(user_id)
        try:
            if mode in ("webhook", "both"):
                send_webhook(event)
            if mode in ("kafka", "both") and producer is not None:
                send_kafka(producer, topic, event)
            sent += 1
        except Exception as ex:
            print(f"ERROR sending event {event.get('eventId')}: {ex}", file=sys.stderr)
        if max_count == 0 or sent < max_count:
            time.sleep(interval)

    if producer is not None:
        producer.flush()
        producer.close()
    print(f"Done. Sent {sent} event(s).")
    return 0


def main() -> int:
    mode = env("SIM_MODE", "webhook").lower()
    currency = env("SIM_CURRENCY", "LP").upper()

    if env_bool("SIM_WAIT_FOR_HEALTH", True):
        wait_for_ledger(env_float("SIM_HEALTH_TIMEOUT", 120.0))

    users = load_customer_ids()
    if not users:
        print("No customers to process (empty list)", file=sys.stderr)
        return 1

    print(f"Loaded {len(users)} customer id(s); sample={users[:3]}{'...' if len(users) > 3 else ''}")

    # --- Phase 1 only: mass wallet backfill ---
    if mode == "backfill":
        summary = backfill_wallets(users, currency)
        # optional smoke events after backfill
        smoke = env_int("SIM_SMOKE_EVENTS_AFTER_BACKFILL", 0)
        if smoke > 0:
            os.environ["SIM_TRANSACTION_COUNT"] = str(smoke)
            return run_events(users, env("SIM_SMOKE_MODE", "webhook"))
        return 0 if summary.get("failed", 0) == 0 else 2

    # --- Phase 2 event modes: optionally onboard first ---
    if env_bool("SIM_ONBOARD_WALLETS", True):
        if len(users) > 100:
            print(f"[onboard] large set ({len(users)}); using batch API")
            backfill_wallets(users, currency)
        else:
            onboard_wallets_one_by_one(users, currency)

    if mode in ("webhook", "kafka", "both"):
        return run_events(users, mode)

    print(f"Unknown SIM_MODE={mode}. Use: backfill | webhook | kafka | both", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
