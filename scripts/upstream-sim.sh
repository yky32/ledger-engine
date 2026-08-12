#!/usr/bin/env bash
# upstream-sim — configurable upstream POS simulator + full greenfield e2e
#
# Default workflow (Wayne):
#   ./scripts/upstream-sim.sh
#     → kill :8080 if needed
#     → start engine with JPA_DDL_AUTO=create
#     → bootstrap policy + digestion rules
#     → fire a mix of txns (earn / filter rejects / signup / redeem / dupe)
#     → print summary + CUST for admin-portal /review
#
# Config (env or scripts/upstream-sim.env):
#   COUNT_PURCHASE_OK=5     good HKD purchases
#   COUNT_PURCHASE_USD=2
#   COUNT_PURCHASE_JPY=2    expect SKIPPED CURRENCY
#   COUNT_TOO_SMALL=1       expect SKIPPED MIN_AMOUNT
#   COUNT_TOO_OLD=1         expect SKIPPED AGE
#   COUNT_SIGNUP=1
#   COUNT_REDEEM=1
#   COUNT_DUPLICATE=1       replay last good eventId
#   AMOUNT / AMOUNT_MIN / AMOUNT_MAX
#   CUST BASE_URL PORT
#
# Flags:
#   --no-restart     use already-running server (still bootstrap unless --no-bootstrap)
#   --no-bootstrap
#   --keep-server    don't kill server we started (default: leave running)
#   --stop-server    stop server we started at end
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck disable=SC1091
[[ -f "$ROOT/scripts/upstream-sim.env" ]] && source "$ROOT/scripts/upstream-sim.env"

BASE_URL="${BASE_URL:-http://localhost:8080}"
PORT="${PORT:-8080}"
CUST="${CUST:-01A$(printf '%08d' $((RANDOM % 100000000)))}"
AMOUNT="${AMOUNT:-200}"
AMOUNT_MIN="${AMOUNT_MIN:-}"
AMOUNT_MAX="${AMOUNT_MAX:-}"
CURRENCY="${CURRENCY:-HKD}"

COUNT_PURCHASE_OK="${COUNT_PURCHASE_OK:-5}"
COUNT_PURCHASE_USD="${COUNT_PURCHASE_USD:-2}"
COUNT_PURCHASE_JPY="${COUNT_PURCHASE_JPY:-2}"
COUNT_TOO_SMALL="${COUNT_TOO_SMALL:-1}"
COUNT_TOO_OLD="${COUNT_TOO_OLD:-1}"
COUNT_SIGNUP="${COUNT_SIGNUP:-1}"
COUNT_REDEEM="${COUNT_REDEEM:-1}"
COUNT_DUPLICATE="${COUNT_DUPLICATE:-1}"

DO_RESTART=1
DO_BOOTSTRAP=1
STOP_SERVER=0
KEEP_SERVER=1
CMD=""
SERVER_PID=""
SERVER_LOG="${SERVER_LOG:-$ROOT/target/upstream-sim-server.log}"
LAST_GOOD_EVENT=""

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[33m%s\033[0m\n' "$*"; }
info() { printf '→ %s\n' "$*"; }

need() { command -v "$1" >/dev/null 2>&1 || { red "missing: $1"; exit 1; }; }
need curl
need jq

for arg in "$@"; do
  case "$arg" in
    --no-restart) DO_RESTART=0 ;;
    --no-bootstrap) DO_BOOTSTRAP=0 ;;
    --stop-server) STOP_SERVER=1; KEEP_SERVER=0 ;;
    --keep-server) KEEP_SERVER=1; STOP_SERVER=0 ;;
    help|-h|--help) CMD=help ;;
    purchase|signup|redeem|bad-jpy|smoke|suite|e2e) CMD="$arg" ;;
  esac
done

# default command = full e2e suite
if [[ -z "$CMD" ]]; then
  CMD=e2e
fi

if [[ "$CMD" == "help" ]]; then
  cat <<EOF
upstream-sim — configurable upstream simulator + greenfield e2e

  ./scripts/upstream-sim.sh                 # DEFAULT: restart w/ ddl=create + suite
  ./scripts/upstream-sim.sh e2e             # same
  ./scripts/upstream-sim.sh suite           # suite only (no restart unless --restart default on e2e)
  ./scripts/upstream-sim.sh --no-restart suite
  ./scripts/upstream-sim.sh purchase        # single txn modes
  ./scripts/upstream-sim.sh bad-jpy
  ./scripts/upstream-sim.sh smoke           # legacy e2e-smoke.sh

Copy env template:
  cp scripts/upstream-sim.env.example scripts/upstream-sim.env

Counts (filter matrix):
  COUNT_PURCHASE_OK=5 COUNT_PURCHASE_JPY=2 COUNT_TOO_OLD=1 \\
    COUNT_TOO_SMALL=1 COUNT_PURCHASE_USD=2 COUNT_SIGNUP=1 \\
    COUNT_REDEEM=1 COUNT_DUPLICATE=1 \\
    ./scripts/upstream-sim.sh

Amount variation:
  AMOUNT_MIN=50 AMOUNT_MAX=800 ./scripts/upstream-sim.sh

After run → admin portal:
  open http://localhost:3000/review  (paste CUST= printed below)
EOF
  exit 0
fi

# --- helpers ---

rand_amount() {
  if [[ -n "$AMOUNT_MIN" && -n "$AMOUNT_MAX" ]]; then
    local lo="$AMOUNT_MIN" hi="$AMOUNT_MAX"
    echo $((lo + RANDOM % (hi - lo + 1)))
  else
    echo "$AMOUNT"
  fi
}

# occurredAt: now | days_ago:N
occurred_iso() {
  local mode="${1:-now}"
  if [[ "$mode" == now ]]; then
    date -u +%Y-%m-%dT%H:%M:%SZ
    return
  fi
  if [[ "$mode" == days_ago:* ]]; then
    local d="${mode#days_ago:}"
    # macOS date vs GNU
    if date -u -v-"${d}"d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null; then
      return
    fi
    date -u -d "${d} days ago" +%Y-%m-%dT%H:%M:%SZ
    return
  fi
  date -u +%Y-%m-%dT%H:%M:%SZ
}

port_pid() {
  lsof -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true
}

stop_port() {
  local p
  p="$(port_pid)"
  if [[ -n "$p" ]]; then
    info "stop process on :$PORT (pid $p)"
    kill $p 2>/dev/null || true
    sleep 1
    p="$(port_pid)"
    if [[ -n "$p" ]]; then
      kill -9 $p 2>/dev/null || true
    fi
  fi
}

wait_health() {
  local i
  info "wait health $BASE_URL/actuator/health"
  for i in $(seq 1 90); do
    if curl -sf "$BASE_URL/actuator/health" >/dev/null 2>&1; then
      green "app up (${i}s)"
      return 0
    fi
    sleep 1
  done
  red "timeout waiting for health — see $SERVER_LOG"
  tail -40 "$SERVER_LOG" 2>/dev/null || true
  exit 1
}

start_server() {
  need mvn
  export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
  export PATH="$JAVA_HOME/bin:$PATH"
  stop_port
  mkdir -p "$(dirname "$SERVER_LOG")"
  info "start engine JPA_DDL_AUTO=create SERVER_PORT=$PORT → log $SERVER_LOG"
  (
    cd "$ROOT"
    # shellcheck disable=SC2030
    export JPA_DDL_AUTO=create
    export SERVER_PORT="$PORT"
    # optional DB still required (Postgres)
    nohup mvn -q spring-boot:run -Dspring-boot.run.jvmArguments="-DJPA_DDL_AUTO=create" \
      >"$SERVER_LOG" 2>&1 &
    echo $! >"$ROOT/target/upstream-sim-server.pid"
  )
  # mvn wraps java — wait on health not pid only
  SERVER_PID="$(cat "$ROOT/target/upstream-sim-server.pid" 2>/dev/null || true)"
  wait_health
}

cleanup() {
  if [[ "$STOP_SERVER" == "1" && -n "${SERVER_PID:-}" ]]; then
    info "stop server (requested)"
    stop_port
  fi
}
trap cleanup EXIT

fire_one() {
  # args: eventType amount currency occurredMode expectStatus(optional)
  local event_type="$1" amount="$2" currency="$3" occurred_mode="${4:-now}" expect="${5:-}"
  local event_id="up-$(date +%s)-$RANDOM"
  local occurred
  occurred="$(occurred_iso "$occurred_mode")"
  local resp status
  resp="$(curl -sS -X POST "$BASE_URL/integrations/webhooks/transactions" \
    -H 'Content-Type: application/json' \
    -d "{
      \"eventId\": \"$event_id\",
      \"associatedIdentifier\": \"$CUST\",
      \"eventType\": \"$event_type\",
      \"amount\": $amount,
      \"currency\": \"$currency\",
      \"occurredAt\": \"$occurred\",
      \"metadata\": {
        \"source\": \"upstream-sim\",
        \"expect\": \"$expect\",
        \"occurredMode\": \"$occurred_mode\"
      }
    }")"
  status="$(echo "$resp" | jq -r '.data.status // "ERR"')"
  local points reason
  points="$(echo "$resp" | jq -r '.data.points // empty')"
  reason="$(echo "$resp" | jq -r '.data.reason // empty')"
  printf '  %-10s %-8s %-4s → %-10s' "$event_type" "$amount" "$currency" "$status"
  if [[ -n "$points" && "$points" != "null" ]]; then printf ' pts=%s' "$points"; fi
  if [[ -n "$reason" && "$reason" != "null" ]]; then printf ' (%s)' "$(echo "$reason" | head -c 60)"; fi
  if [[ -n "$expect" && "$status" != "$expect" ]]; then
    printf '  %s' "$(red "expected $expect")"
    echo
    echo "$resp" | jq .
    return 1
  fi
  echo
  if [[ "$status" == "EARNED" || "$status" == "BURNED" ]]; then
    LAST_GOOD_EVENT="$event_id"
  fi
  # tallies via globals
  case "$status" in
    EARNED) T_EARNED=$((T_EARNED + 1)) ;;
    BURNED) T_BURNED=$((T_BURNED + 1)) ;;
    SKIPPED) T_SKIPPED=$((T_SKIPPED + 1)) ;;
    DUPLICATE) T_DUP=$((T_DUP + 1)) ;;
    *) T_OTHER=$((T_OTHER + 1)) ;;
  esac
  return 0
}

run_suite() {
  T_EARNED=0 T_BURNED=0 T_SKIPPED=0 T_DUP=0 T_OTHER=0
  local i amt

  info "suite CUST=$CUST counts: OK=$COUNT_PURCHASE_OK USD=$COUNT_PURCHASE_USD JPY=$COUNT_PURCHASE_JPY SMALL=$COUNT_TOO_SMALL OLD=$COUNT_TOO_OLD SIGNUP=$COUNT_SIGNUP REDEEM=$COUNT_REDEEM DUPE=$COUNT_DUPLICATE"

  for i in $(seq 1 "$COUNT_PURCHASE_OK"); do
    amt="$(rand_amount)"
    fire_one PURCHASE "$amt" HKD now EARNED || true
  done
  for i in $(seq 1 "$COUNT_PURCHASE_USD"); do
    amt="$(rand_amount)"
    fire_one PURCHASE "$amt" USD now EARNED || true
  done
  for i in $(seq 1 "$COUNT_PURCHASE_JPY"); do
    fire_one PURCHASE 50 JPY now SKIPPED || true
  done
  for i in $(seq 1 "$COUNT_TOO_SMALL"); do
    # default minAmount 0.01 — use 0 for AMOUNT fail or very small; 0 → AMOUNT
    fire_one PURCHASE 0 HKD now SKIPPED || true
  done
  for i in $(seq 1 "$COUNT_TOO_OLD"); do
    fire_one PURCHASE 100 HKD days_ago:30 SKIPPED || true
  done
  for i in $(seq 1 "$COUNT_SIGNUP"); do
    fire_one SIGNUP 0 LP now EARNED || true
  done
  for i in $(seq 1 "$COUNT_REDEEM"); do
    fire_one REDEEM 1 LP now "" || true
  done
  if [[ "$COUNT_DUPLICATE" -gt 0 && -n "$LAST_GOOD_EVENT" ]]; then
    info "duplicate eventId=$LAST_GOOD_EVENT x$COUNT_DUPLICATE"
    for i in $(seq 1 "$COUNT_DUPLICATE"); do
      local resp status
      resp="$(curl -sS -X POST "$BASE_URL/integrations/webhooks/transactions" \
        -H 'Content-Type: application/json' \
        -d "{
          \"eventId\": \"$LAST_GOOD_EVENT\",
          \"associatedIdentifier\": \"$CUST\",
          \"eventType\": \"PURCHASE\",
          \"amount\": 200,
          \"currency\": \"HKD\",
          \"occurredAt\": \"$(occurred_iso now)\"
        }")"
      status="$(echo "$resp" | jq -r '.data.status // "ERR"')"
      printf '  DUPLICATE  replay     → %s\n' "$status"
      if [[ "$status" == "DUPLICATE" ]]; then T_DUP=$((T_DUP + 1)); else T_OTHER=$((T_OTHER + 1)); fi
    done
  fi

  echo
  info "wallet summary"
  curl -sS "$BASE_URL/wallets/${CUST}?currencies=LP,HKD" | jq '{
    cust: (.data.associatedIdentifier // .data.ownerId),
    settlement: .data.settlementCurrency,
    accounts: [.data.accounts[]? | {currency, ledgerBalance, availableBalance}]
  }' 2>/dev/null || true

  info "OPEN fails (sample)"
  curl -sS "$BASE_URL/integrations/failed-transactions?status=OPEN&page=1&size=10" \
    | jq '[.data[]? | {id, eventId, failureCode, reason}]' 2>/dev/null || true

  echo
  green "======== SUITE DONE ========"
  echo "  CUST=$CUST"
  echo "  EARNED=$T_EARNED  BURNED=$T_BURNED  SKIPPED=$T_SKIPPED  DUPLICATE=$T_DUP  OTHER=$T_OTHER"
  echo "  lastGoodEventId=$LAST_GOOD_EVENT"
  echo
  echo "  Admin review:"
  echo "    cd ../ledger-engine-admin-portal && npm run dev"
  echo "    open http://localhost:3000/review  → paste $CUST"
  echo
  if [[ "$KEEP_SERVER" == "1" && "$DO_RESTART" == "1" ]]; then
    yellow "  engine left running on $BASE_URL (log: $SERVER_LOG)"
  fi
}

fire_single_pretty() {
  local event_type="$1" amount="$2" currency="$3"
  local event_id="up-$(date +%s)-$RANDOM"
  local occurred
  occurred="$(occurred_iso now)"
  info "POST $event_type amount=$amount $currency eventId=$event_id"
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
      \"metadata\": { \"source\": \"upstream-sim\" }
    }")"
  echo "$resp" | jq .
  local status
  status="$(echo "$resp" | jq -r '.data.status // empty')"
  curl -sS "$BASE_URL/wallets/${CUST}?currencies=LP" | jq '{
    cust: (.data.associatedIdentifier // .data.ownerId),
    accounts: [.data.accounts[]? | {currency, ledgerBalance, availableBalance}]
  }' 2>/dev/null || true
  if [[ "$status" == "EARNED" || "$status" == "BURNED" || "$status" == "DUPLICATE" ]]; then
    curl -sS "$BASE_URL/integrations/ledger-entries?eventId=${event_id}" | jq '.data // .'
  fi
  if [[ "$status" == "SKIPPED" ]]; then
    curl -sS "$BASE_URL/integrations/failed-transactions?eventId=${event_id}" | jq '.data // .'
  fi
  green "DONE status=$status CUST=$CUST eventId=$event_id"
}

# --- main ---

info "CMD=$CMD BASE_URL=$BASE_URL CUST=$CUST restart=$DO_RESTART"

if [[ "$CMD" == "e2e" || "$CMD" == "suite" ]]; then
  if [[ "$CMD" == "e2e" || "$DO_RESTART" == "1" ]]; then
    if [[ "$CMD" == "suite" && "$DO_RESTART" == "0" ]]; then
      :
    elif [[ "$CMD" == "e2e" ]]; then
      DO_RESTART=1
    fi
  fi
fi

# e2e always restarts unless --no-restart
if [[ "$CMD" == "e2e" && "$DO_RESTART" == "1" ]]; then
  start_server
elif [[ "$DO_RESTART" == "1" && "$CMD" == "suite" ]]; then
  # suite defaults to no restart unless user wants — only e2e forces restart
  if curl -sf "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    green "app already up (suite, no forced restart)"
  else
    start_server
  fi
else
  if ! curl -sf "$BASE_URL/actuator/health" >/dev/null 2>&1; then
    if [[ "$DO_RESTART" == "1" ]]; then
      start_server
    else
      red "app not up at $BASE_URL — run without --no-restart or: mvn spring-boot:run"
      exit 1
    fi
  else
    green "app up"
  fi
fi

if [[ "$DO_BOOTSTRAP" == "1" && "$CMD" != "smoke" ]]; then
  info "bootstrap-runtime"
  BASE_URL="$BASE_URL" "$ROOT/scripts/bootstrap-runtime.sh"
fi

case "$CMD" in
  e2e|suite) run_suite ;;
  smoke)
    BASE_URL="$BASE_URL" "$ROOT/scripts/e2e-smoke.sh"
    ;;
  purchase) fire_single_pretty PURCHASE "$(rand_amount)" "$CURRENCY" ;;
  signup)   fire_single_pretty SIGNUP 0 LP ;;
  redeem)
    local_burn=1
    [[ "$AMOUNT" != "200" ]] && local_burn="$AMOUNT"
    fire_single_pretty REDEEM "$local_burn" LP
    ;;
  bad-jpy)  fire_single_pretty PURCHASE 50 JPY ;;
  *) red "unknown cmd: $CMD"; exit 1 ;;
esac
