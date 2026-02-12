#!/usr/bin/env python3
"""
Seed a sample graph through graph-composer APIs (no direct SQL writes).

Graph shape:
  PlanA -> Task1A, Task1B
  Task1A -> PlanB
  PlanB -> Task2
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from typing import Any


def _request_json(
    base_url: str,
    method: str,
    path: str,
    payload: dict[str, Any] | None = None,
    timeout_seconds: float = 30.0,
) -> Any:
    url = urllib.parse.urljoin(base_url.rstrip("/") + "/", path.lstrip("/"))
    data = None
    headers = {"Accept": "application/json"}

    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url=url, method=method, data=data, headers=headers)

    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            body = response.read().decode("utf-8").strip()
            if not body:
                return None
            return json.loads(body)
    except urllib.error.HTTPError as exc:
        details = exc.read().decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"{method} {url} failed ({exc.code}): {details}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"{method} {url} failed: {exc.reason}") from exc


def _iso_to_datetime(value: str) -> datetime:
    # Supports both "Z" and "+00:00" suffix styles.
    normalized = value.replace("Z", "+00:00")
    return datetime.fromisoformat(normalized)


def _find_or_create_graph(base_url: str, tenant_id: str, graph_name: str, timeout_seconds: float) -> str:
    tenant_query = urllib.parse.urlencode({"tenantId": tenant_id})
    summaries = _request_json(
        base_url=base_url,
        method="GET",
        path=f"/api/v1/graphs?{tenant_query}",
        timeout_seconds=timeout_seconds,
    )

    matches = [summary for summary in summaries if summary.get("name") == graph_name]
    if matches:
        matches.sort(key=lambda item: _iso_to_datetime(item.get("updatedAt", "1970-01-01T00:00:00")), reverse=True)
        return matches[0]["id"]

    created = _request_json(
        base_url=base_url,
        method="POST",
        path="/api/v1/graphs",
        payload={"name": graph_name, "tenantId": tenant_id},
        timeout_seconds=timeout_seconds,
    )
    graph_id = created.get("id")
    if not graph_id:
        raise RuntimeError("Create graph response did not include an id")
    return graph_id


def _build_graph_payload(graph_id: str, tenant_id: str, graph_name: str) -> dict[str, Any]:
    plan_a_py = """from agentic_common.pb import PlanInput, PlanResult


def plan(plan_input: PlanInput) -> PlanResult:
    result = PlanResult()
    result.next_task_names.extend(["Task1A", "Task1B"])
    return result
"""

    plan_b_py = """from agentic_common.pb import PlanInput, PlanResult


def plan(plan_input: PlanInput) -> PlanResult:
    result = PlanResult()
    result.next_task_names.append("Task2")
    return result
"""

    task_template = """from agentic_common.pb import TaskInput, TaskResult


def task(task_input: TaskInput) -> TaskResult:
    result = TaskResult()
    result.id = task_input.input_id or "{fallback_id}"
    return result
"""

    return {
        "id": graph_id,
        "name": graph_name,
        "tenantId": tenant_id,
        "status": "NEW",
        "plans": [
            {
                "name": "PlanA",
                "label": "Plan A",
                "files": [
                    {"name": "plan.py", "contents": plan_a_py},
                    {"name": "requirements.txt", "contents": "# Dependencies for PlanA\n"},
                ],
            },
            {
                "name": "PlanB",
                "label": "Plan B",
                "files": [
                    {"name": "plan.py", "contents": plan_b_py},
                    {"name": "requirements.txt", "contents": "# Dependencies for PlanB\n"},
                ],
            },
        ],
        "tasks": [
            {
                "name": "Task1A",
                "label": "Task 1A",
                "files": [
                    {"name": "task.py", "contents": task_template.format(fallback_id="Task1A-result")},
                    {"name": "requirements.txt", "contents": "# Dependencies for Task1A\n"},
                ],
            },
            {
                "name": "Task1B",
                "label": "Task 1B",
                "files": [
                    {"name": "task.py", "contents": task_template.format(fallback_id="Task1B-result")},
                    {"name": "requirements.txt", "contents": "# Dependencies for Task1B\n"},
                ],
            },
            {
                "name": "Task2",
                "label": "Task 2",
                "files": [
                    {"name": "task.py", "contents": task_template.format(fallback_id="Task2-result")},
                    {"name": "requirements.txt", "contents": "# Dependencies for Task2\n"},
                ],
            },
        ],
        "edges": [
            {"from": "PlanA", "fromType": "PLAN", "to": "Task1A", "toType": "TASK"},
            {"from": "PlanA", "fromType": "PLAN", "to": "Task1B", "toType": "TASK"},
            {"from": "Task1A", "fromType": "TASK", "to": "PlanB", "toType": "PLAN"},
            {"from": "PlanB", "fromType": "PLAN", "to": "Task2", "toType": "TASK"},
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Load sample graph through graph-composer API")
    parser.add_argument(
        "--base-url",
        default="http://localhost:8088",
        help="Graph composer base URL (default: http://localhost:8088)",
    )
    parser.add_argument(
        "--tenant-id",
        default="tenant-dev",
        help="Tenant id (default: tenant-dev)",
    )
    parser.add_argument(
        "--graph-name",
        default="sample-graph-plan-a-plan-b",
        help="Graph name to create or update (default: sample-graph-plan-a-plan-b)",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=float,
        default=30.0,
        help="HTTP timeout per request in seconds (default: 30)",
    )
    parser.add_argument(
        "--execute",
        action="store_true",
        help="Immediately start graph execution after loading",
    )
    args = parser.parse_args()

    graph_id = _find_or_create_graph(args.base_url, args.tenant_id, args.graph_name, args.timeout_seconds)
    payload = _build_graph_payload(graph_id, args.tenant_id, args.graph_name)

    _request_json(
        base_url=args.base_url,
        method="PUT",
        path=f"/api/v1/graphs/{graph_id}",
        payload=payload,
        timeout_seconds=args.timeout_seconds,
    )

    _request_json(
        base_url=args.base_url,
        method="PUT",
        path=f"/api/v1/graphs/{graph_id}/status",
        payload={"status": "ACTIVE"},
        timeout_seconds=args.timeout_seconds,
    )

    tenant_query = urllib.parse.urlencode({"tenantId": args.tenant_id})
    execution_response = None
    if args.execute:
        execution_response = _request_json(
            base_url=args.base_url,
            method="POST",
            path=f"/api/v1/graphs/{graph_id}/execute?{tenant_query}",
            timeout_seconds=args.timeout_seconds,
        )

    saved_graph = _request_json(
        base_url=args.base_url,
        method="GET",
        path=f"/api/v1/graphs/{graph_id}?{tenant_query}",
        timeout_seconds=args.timeout_seconds,
    )

    plans_count = len(saved_graph.get("plans", []))
    tasks_count = len(saved_graph.get("tasks", []))
    print(f"Seeded graph id={graph_id} tenant={args.tenant_id} name={args.graph_name} plans={plans_count} tasks={tasks_count}")
    if execution_response:
        execution_id = execution_response.get("executionId", "")
        status = execution_response.get("status", "")
        print(f"Execution started lifetime_id={execution_id} status={status}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        raise SystemExit(1)
