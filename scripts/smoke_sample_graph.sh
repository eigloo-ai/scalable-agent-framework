#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8088"
DATA_PLANE_URL="http://localhost:8081"
TENANT_ID="tenant-dev"
GRAPH_NAME="sample-graph-plan-a-plan-b-e2e-$(date +%s)"
TIMEOUT_SECONDS=300
STARTUP_TIMEOUT_SECONDS=""
POLL_INTERVAL_SECONDS=2
START_STACK=false
BUILD_IMAGES=false

usage() {
  cat <<'USAGE'
Usage: ./scripts/smoke_sample_graph.sh [options]

Options:
  --base-url URL            Graph composer URL (default: http://localhost:8088)
  --data-plane-url URL      Data plane URL (default: http://localhost:8081)
  --tenant-id TENANT        Tenant id (default: tenant-dev)
  --graph-name NAME         Graph name (default: sample-graph-plan-a-plan-b-e2e-<timestamp>)
  --timeout-seconds N       Timeout for full run completion (default: 300)
  --startup-timeout-seconds N
                            Timeout for startup health checks (default: --timeout-seconds)
  --poll-interval-seconds N Poll interval while waiting for run completion (default: 2)
  --start-stack             Start required Docker services before smoke run
  --build-images            When used with --start-stack, build images before start
  -h, --help                Show this help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="$2"
      shift 2
      ;;
    --data-plane-url)
      DATA_PLANE_URL="$2"
      shift 2
      ;;
    --tenant-id)
      TENANT_ID="$2"
      shift 2
      ;;
    --graph-name)
      GRAPH_NAME="$2"
      shift 2
      ;;
    --timeout-seconds)
      TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    --startup-timeout-seconds)
      STARTUP_TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    --poll-interval-seconds)
      POLL_INTERVAL_SECONDS="$2"
      shift 2
      ;;
    --start-stack)
      START_STACK=true
      shift
      ;;
    --build-images)
      BUILD_IMAGES=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$STARTUP_TIMEOUT_SECONDS" ]]; then
  STARTUP_TIMEOUT_SECONDS="$TIMEOUT_SECONDS"
fi

dump_runtime_diagnostics() {
  if ! command -v docker >/dev/null 2>&1; then
    return
  fi

  echo "==== docker compose ps ====" >&2
  docker compose ps >&2 || true

  for service in graph-composer data-plane control-plane executor-java kafka postgres; do
    echo "==== docker logs --tail 120 ${service} ====" >&2
    docker logs --tail 120 "${service}" >&2 || true
  done
}

wait_for_http() {
  local url="$1"
  local timeout="$2"
  local start_ts
  start_ts=$(date +%s)
  while true; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    if (( $(date +%s) - start_ts >= timeout )); then
      echo "Timed out waiting for endpoint: $url" >&2
      dump_runtime_diagnostics
      return 1
    fi
    sleep 2
  done
}

if [[ "$START_STACK" == "true" ]]; then
  compose_args=(-d)
  if [[ "$BUILD_IMAGES" == "true" ]]; then
    compose_args+=(--build)
  fi
  docker compose up "${compose_args[@]}" kafka postgres graph-composer executor-java data-plane control-plane
fi

wait_for_http "${BASE_URL%/}/actuator/health" "$STARTUP_TIMEOUT_SECONDS"
wait_for_http "${DATA_PLANE_URL%/}/actuator/health" "$STARTUP_TIMEOUT_SECONDS"
wait_for_http "http://localhost:8082/actuator/health" "$STARTUP_TIMEOUT_SECONDS"
wait_for_http "http://localhost:8083/actuator/health" "$STARTUP_TIMEOUT_SECONDS"

echo "Seeding and executing sample graph tenant=${TENANT_ID} graph=${GRAPH_NAME}"
load_output="$("./scripts/load_sample_graph.sh" \
  --base-url "$BASE_URL" \
  --tenant-id "$TENANT_ID" \
  --graph-name "$GRAPH_NAME" \
  --execute)"
echo "$load_output"

graph_id="$(printf '%s\n' "$load_output" | sed -n 's/^Seeded graph id=\([^ ]*\).*/\1/p' | tail -1)"
lifetime_id="$(printf '%s\n' "$load_output" | sed -n 's/^Execution started lifetime_id=\([^ ]*\).*/\1/p' | tail -1)"

if [[ -z "$graph_id" || -z "$lifetime_id" ]]; then
  echo "Failed to parse graph_id or lifetime_id from load_sample_graph output" >&2
  exit 1
fi

echo "Waiting for run completion graph_id=${graph_id} lifetime_id=${lifetime_id}"

deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))
timeline_url="${DATA_PLANE_URL%/}/api/v1/runs/${lifetime_id}/timeline?tenantId=${TENANT_ID}&graphId=${graph_id}"
latest_timeline=""

while (( $(date +%s) < deadline )); do
  timeline_json="$(curl -fsS "$timeline_url" 2>/dev/null || true)"
  if [[ -z "$timeline_json" ]]; then
    sleep "$POLL_INTERVAL_SECONDS"
    continue
  fi

  latest_timeline="$timeline_json"
  mapfile -t parsed < <(python3 - "$timeline_json" <<'PY'
import json
import sys

payload = json.loads(sys.argv[1])
status = payload.get("status", "")
events = payload.get("events", [])
succeeded_plan_names = {
    event.get("nodeName")
    for event in events
    if event.get("eventType") == "PLAN_EXECUTION" and event.get("status") == "EXECUTION_STATUS_SUCCEEDED"
}
succeeded_task_names = {
    event.get("nodeName")
    for event in events
    if event.get("eventType") == "TASK_EXECUTION" and event.get("status") == "EXECUTION_STATUS_SUCCEEDED"
}
expected_plans = {"PlanA", "PlanB"}
expected_tasks = {"Task1A", "Task1B", "Task2"}
missing_plans = ",".join(sorted(expected_plans - succeeded_plan_names)) or "-"
missing_tasks = ",".join(sorted(expected_tasks - succeeded_task_names)) or "-"
print(status)
print(missing_plans)
print(missing_tasks)
PY
  )

  run_status="${parsed[0]}"
  missing_plans="${parsed[1]}"
  missing_tasks="${parsed[2]}"

  if [[ "$run_status" == "FAILED" ]]; then
    echo "Run failed. Timeline payload:" >&2
    echo "$timeline_json" >&2
    exit 1
  fi

  if [[ "$run_status" == "SUCCEEDED" && "$missing_plans" == "-" && "$missing_tasks" == "-" ]]; then
    echo "Smoke run succeeded with full node coverage."
    echo "timeline_url=${timeline_url}"
    exit 0
  fi

  sleep "$POLL_INTERVAL_SECONDS"
done

echo "Timed out waiting for successful run completion." >&2
if [[ -n "$latest_timeline" ]]; then
  echo "$latest_timeline" >&2
fi
exit 1
