#!/usr/bin/env bash
# upstream-sim — one-command local "play upstream POS" against ledger-engine.
#
# Prerequisites: app already running (mvn spring-boot:run / docker).
#
# Usage:
#   ./scripts/upstream-sim.sh
#   ./scripts/upstream-sim.sh --no-bootstrap
#   CUST=01A11112222 AMOUNT=500 ./scripts/upstream-sim.sh
#   BASE_URL=http://localhost:8080 ./scripts/upstream-sim.sh purchase
#   ./scripts/upstream-sim.sh signup
#   ./scripts/upstream-sim.sh bad-jpy          # expect SKIPPED
#   ./scripts/upstream-sim.sh help
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
CUST="${CUST:-01A$(printf '%08d' $((RANDOM % 100000000)))}"
AMOUNT="${AMOUNT:-200}"
CURRENCY="${CURRENCY:-HKD}"
DO_BOOTSTRAP=1
CMD="${1:-purchase}"

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
info() { printf '→ %s\n' "$*"; }

need() { command -v "$1" >/dev/null 2>&1 || { red "missing: $1"; exit 1; }; }
need curl
need jq

for arg in "$@"; do
  case "$arg" in
    --no-bootstrap) DO_BOOTSTRAP=0 ;;
    help|-h|--help) CMD=help ;;
    purchase|signup|redeem|bad-jpy|smoke) CMD="$arg" ;;
  esac
done

if [[ "$CMD" == "help" ]]; then
  cat <<EOF
upstream-sim — pretend you are the upstream system (POS / order).

  ./scripts/upstream-sim.sh                 # bootstrap + PURCHASE 200 HKD
  ./scripts/upstream-sim.sh --no-bootstrap  # skip bootstrap
  ./scripts/upstream-sim.sh signup
  ./scripts/upstream-sim.sh redeem          # needs LP balance first
  ./scripts/upstream-sim.sh bad-jpy         # currency fail demo
  ./scripts/upstream-sim.sh smoke           # full e2e-smoke.sh

Env:
  BASE_URL   default http://localhost:8080
  CUST       default random 01A########
  AMOUNT     default 200
  CURRENCY   default HKD
EOF
  exit 0
fi

info "BASE_URL=$BASE_URL CUST=$CUST CMD=$CMD"

info "health"
curl -sf "$BASE_URL/actuator/health" >/dev/null \
  || { red "app not up at $BASE_URL — run: mvn spring-boot:run"; exit 1; }
green "app up"

if [[ "$DO_BOOTSTRAP" == "1" && "$CMD" != "smoke" ]]; then
  info "bootstrap-runtime"
  BASE_URL="$BASE_URL" "$ROOT/scripts/bootstrap-runtime.sh"
fi

if [[ "$CMD" == "smoke" ]]; then
  BASE_URL="$BASE_URL" "$ROOT/scripts/e2e-smoke.sh"
  exit 0
fi

fire() {
  local event_type="$1" amount="$2" currency="$3"
  local event_id="up-$(date +%s)-$RANDOM"
  local occurred
  occurred="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  info "POST /integrations/webhooks/transactions eventType=$event_type amount=$amount $currency eventId=$event_id"
  local resp
  resp="$(curl -sS -X POST "$BASE_URL/integrations/webhooks/transactions" \
    -H 'Content-Type: application/json' \
    -d "{
      \"eventId\": \"$event_id\",
      \"associatedIdentifier\": \"$CUST\",
      \"eventType\": \"$event_type\",
      \"amount\": $amount,
      \"currency\": \"$currency\",
      \"occurredAt\": \"$occurred\",
      \"metadata\": { \"source\": \"upstream-sim\", \"cli\": true }
    }")"
  echo "$resp" | jq .
  local status
  status="$(echo "$resp" | jq -r '.data.status // empty')"
  if [[ -z "$status" ]]; then
    red "unexpected response (no data.status)"
    exit 1
  fi

  info "wallet LP"
  curl -sS "$BASE_URL/wallets/${CUST}?currencies=LP" | jq '{
    id: .data.associatedIdentifier // .data.ownerId,
    settlement: .data.settlementCurrency,
    accounts: [.data.accounts[]? | {currency, ledgerBalance, availableBalance}]
  }' 2>/dev/null || curl -sS "$BASE_URL/wallets/${CUST}?currencies=LP" | jq .

  if [[ "$status" == "EARNED" || "$status" == "BURNED" || "$status" == "DUPLICATE" ]]; then
    info "legs?eventId=$event_id"
    curl -sS "$BASE_URL/integrations/ledger-entries?eventId=${event_id}" | jq '.data // .'
  fi

  if [[ "$status" == "SKIPPED" ]]; then
    info "failed-transactions?eventId=$event_id"
    curl -sS "$BASE_URL/integrations/failed-transactions?eventId=${event_id}" | jq '.data // .'
  fi

  echo
  green "DONE status=$status cust=$CUST eventId=$event_id"
  echo "  re-run same style: CUST=$CUST ./scripts/upstream-sim.sh --no-bootstrap"
}

case "$CMD" in
  purchase) fire PURCHASE "$AMOUNT" "$CURRENCY" ;;
  signup)   fire SIGNUP 0 LP ;;
  redeem)
    # default burn 1 LP if AMOUNT still 200 — use AMOUNT for LP points to burn
    local_burn="${AMOUNT}"
    if [[ "$AMOUNT" == "200" ]]; then local_burn=1; fi
    fire REDEEM "$local_burn" LP
    ;;
  bad-jpy)  fire PURCHASE 50 JPY ;;
  *) red "unknown cmd: $CMD (try help)"; exit 1 ;;
esac
