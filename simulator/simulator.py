#!/usr/bin/env python3
"""Simulates an external system sending transactional events via webhook and/or Kafka."""

import json
import os
import random
import sys
import time
import uuid
from datetime import datetime, timezone

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


def ledger_base_url() -> str:
    webhook = env("SIM_WEBHOOK_URL", "http://app:8080/integrations/webhooks/transactions")
    marker = "/integrations/webhooks"
    if marker in webhook:
        return webhook.split(marker, 1)[0]
    return env("SIM_LEDGER_BASE_URL", "http://app:8080")


def onboard_wallets(users: list[str], currency: str) -> None:
    if env("SIM_ONBOARD_WALLETS", "true").lower() not in ("1", "true", "yes"):
        print("Wallet onboarding skipped (SIM_ONBOARD_WALLETS=false)")
        return
    base = ledger_base_url()
    for user_id in users:
        url = f"{base}/wallets"
        payload = {"userId": user_id, "currency": currency, "name": f"Sim wallet {user_id}"}
        response = requests.post(url, json=payload, timeout=10)
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
    response = requests.post(url, json=event, timeout=10)
    response.raise_for_status()
    print(f"[webhook] {event['eventId']} user={event['userId']} amount={event['amount']} -> {response.json()}")


def send_kafka(producer, topic: str, event: dict) -> None:
    payload = json.dumps(event).encode("utf-8")
    producer.send(topic, key=event["eventId"].encode("utf-8"), value=payload).get(timeout=10)
    print(f"[kafka]   {event['eventId']} user={event['userId']} amount={event['amount']} -> topic={topic}")


def main() -> int:
    mode = env("SIM_MODE", "webhook").lower()
    interval = env_float("SIM_INTERVAL_SECONDS", 2.0)
    max_count = env_int("SIM_TRANSACTION_COUNT", 20)
    user_prefix = env("SIM_USER_ID_PREFIX", "CUST-")
    user_count = env_int("SIM_USER_COUNT", 5)

    users = [f"{user_prefix}{i:04d}" for i in range(1, user_count + 1)]
    currency = env("SIM_CURRENCY", "LP")
    onboard_wallets(users, currency)

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
    print(f"Simulator mode={mode} interval={interval}s count={max_count or 'unlimited'} currency={env('SIM_CURRENCY', 'LP')}")
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


if __name__ == "__main__":
    raise SystemExit(main())
