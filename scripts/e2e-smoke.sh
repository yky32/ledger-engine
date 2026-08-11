#!/usr/bin/env bash
# E2E smoke: (optional onboard) → earn webhook → query LP → fail-path check.
# Requires: app on BASE_URL, jq, curl. Postgres already up for the app.
#
# SKIP_ONBOARD=1  → rely on auto-create-wallet (default true): first webhook provisions HKD+LP
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CUST_ID="${CUST_ID:-01A$(printf '%08d' $((RANDOM % 100000000)))}"
AMOUNT="${AMOUNT:-200}"
CURRENCY="${CURRENCY:-HKD}"
RATE_EXPECT="${RATE_EXPECT:-2}"
SKIP_ONBOARD="${SKIP_ONBOARD:-0}"

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
info() { printf '→ %s\n' "$*"; }

need() { command -v "$1" >/dev/null 2>&1 || { red "missing dependency: $1"; exit 1; }; }
need curl
need jq

info "BASE_URL=$BASE_URL CUST_ID=$CUST_ID SKIP_ONBOARD=$SKIP_ONBOARD"

info "0) health"
curl -sf "$BASE_URL/actuator/health" | jq -e '.status=="UP"' >/dev/null \
  || curl -sf "$BASE_URL/actuator/health" >/dev/null \
  || { red "app not healthy at $BASE_URL"; exit 1; }
green "app up"

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
green "earned points=$POINTS (expect ~$RATE_EXPECT)"

info "3) query wallet LP books"
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
green "query LP ok"

info "4) fail-path: ineligible currency → failed-transactions API"
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

FAILS=$(curl -sS "$BASE_URL/integrations/failed-transactions?associatedIdentifier=${CUST_ID}&failureCode=CURRENCY&limit=10")
echo "$FAILS" | jq .
echo "$FAILS" | jq -e --arg e "$BAD_EVENT" '[.data[] | select(.eventId==$e)] | length > 0' >/dev/null \
  || { red "failed-transactions API missing event $BAD_EVENT"; exit 1; }
green "failed-ingest query ok"

echo
green "E2E SMOKE PASSED cust=$CUST_ID event=$EVENT_ID points=$POINTS lp_bal=$LP_BAL skip_onboard=$SKIP_ONBOARD"
