#!/usr/bin/env bash
# Bootstrap runtime DB config for a fresh ledger-engine (no YAML seed).
# Idempotent: safe to re-run.
#
# Ensures:
#   1) IngestPolicy via PUT /ingest-policy
#   2) Digestion rules PURCHASE_DEFAULT / SIGNUP_DEFAULT / REDEEM_DEFAULT
#
#   ./scripts/bootstrap-runtime.sh
#   BASE_URL=http://localhost:8080 ./scripts/bootstrap-runtime.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
info() { printf '→ %s\n' "$*"; }

need() { command -v "$1" >/dev/null 2>&1 || { red "missing: $1"; exit 1; }; }
need curl
need jq

info "BASE_URL=$BASE_URL"

info "0) health"
curl -sf "$BASE_URL/actuator/health" >/dev/null \
  || { red "app not healthy at $BASE_URL"; exit 1; }
green "app up"

info "1) ingest-policy (door)"
POLICY=$(curl -sS -X PUT "$BASE_URL/ingest-policy" \
  -H 'Content-Type: application/json' \
  -d '{
    "isEnabled": true,
    "isAutoCreateWallet": true,
    "autoWalletSettlementCurrency": "HKD",
    "autoWalletEnsureCurrency": "LP",
    "autoWalletAssociatedFrom": "CRM",
    "autoWalletNamePrefix": "Auto "
  }')
echo "$POLICY" | jq -e '.code=="SYS0000"' >/dev/null || { red "ingest-policy failed: $POLICY"; exit 1; }
echo "$POLICY" | jq '.data | {isEnabled,isAutoCreateWallet,autoWalletSettlementCurrency,autoWalletEnsureCurrency}'
green "ingest-policy ok"

# $1=code $2=json body without code
upsert_rule() {
  local code="$1"
  local fields="$2"
  local existing id resp
  existing=$(curl -sS "$BASE_URL/digestion-rules?code=$(printf %s "$code" | jq -sRr @uri)")
  if echo "$existing" | jq -e --arg c "$code" '.code=="SYS0000" and .data.code==$c' >/dev/null 2>&1; then
    id=$(echo "$existing" | jq -r '.data.id')
    info "update digestion-rule $code id=$id"
    resp=$(curl -sS -X PUT "$BASE_URL/digestion-rules/${id}" \
      -H 'Content-Type: application/json' \
      -d "$fields")
    echo "$resp" | jq -e '.code=="SYS0000"' >/dev/null || { red "PUT $code failed: $resp"; exit 1; }
    green "updated $code"
  else
    info "create digestion-rule $code"
    resp=$(curl -sS -X POST "$BASE_URL/digestion-rules" \
      -H 'Content-Type: application/json' \
      -d "$(echo "$fields" | jq -c --arg c "$code" '. + {code:$c}')")
    if echo "$resp" | jq -e '.code=="SYS0000"' >/dev/null 2>&1; then
      green "created $code"
    elif echo "$resp" | jq -e '.code=="DIG0409"' >/dev/null 2>&1; then
      info "code conflict — fetch and PUT"
      existing=$(curl -sS "$BASE_URL/digestion-rules?code=$(printf %s "$code" | jq -sRr @uri)")
      id=$(echo "$existing" | jq -r '.data.id')
      resp=$(curl -sS -X PUT "$BASE_URL/digestion-rules/${id}" \
        -H 'Content-Type: application/json' \
        -d "$fields")
      echo "$resp" | jq -e '.code=="SYS0000"' >/dev/null || { red "PUT after conflict failed: $resp"; exit 1; }
      green "updated $code after conflict"
    else
      red "create $code failed: $resp"; exit 1
    fi
  fi
}

info "2) digestion-rules (brain)"

upsert_rule PURCHASE_DEFAULT '{
  "name": "Purchase earn 1%",
  "eventType": "PURCHASE",
  "operation": "EARN",
  "isEnabled": true,
  "priority": 10,
  "minAmount": 0.01,
  "eligibleCurrencies": ["HKD","USD"],
  "maxAgeDays": 7,
  "pointCurrency": "LP",
  "formula": "RATE:0.01"
}'

upsert_rule SIGNUP_DEFAULT '{
  "name": "Signup fixed LP",
  "eventType": "SIGNUP",
  "operation": "EARN",
  "isEnabled": true,
  "priority": 20,
  "minAmount": 0,
  "pointCurrency": "LP",
  "formula": "FIXED:100"
}'

upsert_rule REDEEM_DEFAULT '{
  "name": "Redeem burn",
  "eventType": "REDEEM",
  "operation": "BURN",
  "isEnabled": true,
  "priority": 30,
  "minAmount": 1,
  "pointCurrency": "LP",
  "formula": "AMOUNT"
}'

info "3) summary"
curl -sS "$BASE_URL/ingest-policy" | jq '.data | {isEnabled,isAutoCreateWallet}'
curl -sS "$BASE_URL/digestion-rules?enabledOnly=true" | jq '[.data[]? | {code,eventType,operation,formula,priority}]'

echo
green "BOOTSTRAP OK — ready for earn webhook / ./scripts/e2e-smoke.sh"
