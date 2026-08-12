#!/usr/bin/env bash
# E2E smoke: bootstrap runtime → (optional onboard) → earn → legs → fail API.
# Requires: app on BASE_URL, jq, curl.
#
# SKIP_ONBOARD=1  → auto-wallet on first eligible webhook
# SKIP_BOOTSTRAP=1 → do not run bootstrap-runtime.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CUST_ID="${CUST_ID:-01A$(printf '%08d' $((RANDOM % 100000000)))}"
AMOUNT="${AMOUNT:-200}"
CURRENCY="${CURRENCY:-HKD}"
RATE_EXPECT="${RATE_EXPECT:-2}"
SKIP_ONBOARD="${SKIP_ONBOARD:-0}"
SKIP_BOOTSTRAP="${SKIP_BOOTSTRAP:-0}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
info() { printf '→ %s\n' "$*"; }

need() { command -v "$1" >/dev/null 2>&1 || { red "missing dependency: $1"; exit 1; }; }
need curl
need jq

info "BASE_URL=$BASE_URL CUST_ID=$CUST_ID SKIP_ONBOARD=$SKIP_ONBOARD SKIP_BOOTSTRAP=$SKIP_BOOTSTRAP"

info "0) health"
curl -sf "$BASE_URL/actuator/health" >/dev/null \
  || { red "app not healthy at $BASE_URL"; exit 1; }
green "app up"

if [[ "$SKIP_BOOTSTRAP" != "1" ]]; then
  info "0b) bootstrap ingest-policy + digestion rules"
  BASE_URL="$BASE_URL" "$ROOT/scripts/bootstrap-runtime.sh"
else
  info "0b) skip bootstrap"
fi

if [[ "$SKIP_ONBOARD" != "1" ]]; then
  info "1) onboard wallet HKD + LP"
  ONBOARD=$(curl -sS -X POST "$BASE_URL/wallets" \
    -H 'Content-Type: application/json' \
    -d "{
      \"associatedIdentifier\": \"$CUST_ID\",
      \"settlementCurrency\": \"HKD\",
      \"name\": \"Smoke $CUST_ID\",
      \"associatedFrom\": \"CRM\",
      \"accounts\": [{ \"currency\": \"LP\", \"name\": \"Loyalty\", \"refCode\": \"LP\" }]
    }")
  echo "$ONBOARD" | jq -e '.code=="SYS0000"' >/dev/null || {
    echo "$ONBOARD" | jq -e '.code=="WAL0409"' >/dev/null && info "wallet already exists" || {
      red "onboard failed: $ONBOARD"; exit 1;
    }
  }
  green "onboard ok"
else
  info "1) skip onboard — auto-create-wallet on first eligible webhook"
fi

EVENT_ID="smoke-$(date +%s)-$RANDOM"
OCCURRED=$(date -u +%Y-%m-%dT%H:%M:%SZ)
info "2) earn webhook eventId=$EVENT_ID amount=$AMOUNT $CURRENCY"
EARN=$(curl -sS -X POST "$BASE_URL/integrations/webhooks/transactions" \
  -H 'Content-Type: application/json' \
  -d "{
    \"eventId\": \"$EVENT_ID\",
    \"associatedIdentifier\": \"$CUST_ID\",
    \"eventType\": \"PURCHASE\",
    \"amount\": $AMOUNT,
    \"currency\": \"$CURRENCY\",
    \"occurredAt\": \"$OCCURRED\"
  }")
echo "$EARN" | jq .
echo "$EARN" | jq -e '.data.status=="EARNED"' >/dev/null || { red "earn not EARNED"; exit 1; }
POINTS=$(echo "$EARN" | jq -r '.data.points')
LEGS=$(echo "$EARN" | jq '.data.legs | length')
echo "$EARN" | jq -e '.data.legs | length == 2' >/dev/null \
  || { red "expected 2 DE legs, got $LEGS"; exit 1; }
MOVEMENT_ID=$(echo "$EARN" | jq -r '.data.movementId')
green "earned points=$POINTS legs=2 movementId=$MOVEMENT_ID (expect ~$RATE_EXPECT pts)"

info "3) query wallet LP + ledger-entries?eventId="
WALLET=$(curl -sS "$BASE_URL/wallets/${CUST_ID}?currencies=LP")
echo "$WALLET" | jq '{associatedIdentifier: .data.associatedIdentifier, settlementCurrency: .data.settlementCurrency, accounts: .data.accounts}'
echo "$WALLET" | jq -e '.data.associatedIdentifier=="'"$CUST_ID"'"' >/dev/null
LP_BAL=$(echo "$WALLET" | jq -r '[.data.accounts[]? | select(.currency=="LP") | .ledgerBalance] | add // 0')
info "LP ledgerBalance sum=$LP_BAL"
python3 - <<PY
from decimal import Decimal
bal = Decimal(str("$LP_BAL"))
pts = Decimal(str("$POINTS"))
assert bal >= pts, f"LP bal {bal} < points {pts}"
print("LP balance check ok")
PY

ENTRIES=$(curl -sS "$BASE_URL/integrations/ledger-entries?eventId=${EVENT_ID}")
echo "$ENTRIES" | jq -e '.data | length == 2' >/dev/null \
  || { red "ledger-entries?eventId= expected 2 legs"; echo "$ENTRIES" | jq .; exit 1; }
if [[ "$MOVEMENT_ID" != "null" && -n "$MOVEMENT_ID" ]]; then
  curl -sS "$BASE_URL/integrations/ledger-entries?movementId=${MOVEMENT_ID}" \
    | jq -e '.data | length == 2' >/dev/null \
    || { red "ledger-entries?movementId= failed"; exit 1; }
fi
green "query LP + legs ok"

info "4) fail-path: JPY → failed-transactions?eventId="
BAD_EVENT="smoke-bad-$RANDOM"
BAD=$(curl -sS -X POST "$BASE_URL/integrations/webhooks/transactions" \
  -H 'Content-Type: application/json' \
  -d "{
    \"eventId\": \"$BAD_EVENT\",
    \"associatedIdentifier\": \"$CUST_ID\",
    \"eventType\": \"PURCHASE\",
    \"amount\": 50,
    \"currency\": \"JPY\",
    \"occurredAt\": \"$OCCURRED\"
  }")
echo "$BAD" | jq -e '.data.status=="SKIPPED"' >/dev/null || { red "expected SKIPPED for JPY"; exit 1; }

FAILS=$(curl -sS "$BASE_URL/integrations/failed-transactions?eventId=${BAD_EVENT}")
echo "$FAILS" | jq .
echo "$FAILS" | jq -e --arg e "$BAD_EVENT" '[.data[] | select(.eventId==$e)] | length > 0' >/dev/null \
  || { red "failed-transactions?eventId= missing $BAD_EVENT"; exit 1; }
green "failed-ingest query ok"

echo
green "E2E SMOKE PASSED cust=$CUST_ID event=$EVENT_ID points=$POINTS lp_bal=$LP_BAL legs=2 skip_onboard=$SKIP_ONBOARD"
