# Sample Graph Docker Runbook

This runbook loads the sample graph through `graph-composer`, starts execution, and traces execution across:

- graph-composer
- executor-java
- data-plane
- control-plane

## 1) Start required services

```bash
docker compose up -d kafka postgres graph-composer executor-java data-plane control-plane
```

## 2) Load and execute the sample graph

```bash
./scripts/load_sample_graph.sh \
  --base-url http://localhost:8088 \
  --tenant-id tenant-dev \
  --graph-name sample-graph-plan-a-plan-b \
  --execute
```

Expected output includes:

- `Seeded graph id=<graph-id> ...`
- `Execution started lifetime_id=<lifetime-id> status=RUNNING`

## 3) Follow end-to-end execution logs

```bash
docker compose logs -f graph-composer executor-java data-plane control-plane
```

Filter to one run:

```bash
docker compose logs -f graph-composer executor-java data-plane control-plane \
  | rg "<lifetime-id>|<graph-id>"
```

## 4) Query run timeline API (recommended)

```bash
curl -s "http://localhost:8081/api/v1/runs/<lifetime-id>/timeline?tenantId=tenant-dev&graphId=<graph-id>" | jq
```

Expected run status progression:

- `QUEUED` right after execute request acceptance
- `RUNNING` while plan/task messages are flowing
- `SUCCEEDED` when emitted edges are fully resolved
- `FAILED` if any persisted plan/task execution fails

## 5) Verify persisted execution records in PostgreSQL

```bash
docker compose exec -T postgres psql -U agentic -d agentic \
  -c "select created_at, name, exec_id, graph_id, lifetime_id, status from plan_executions where tenant_id='tenant-dev' and lifetime_id='<lifetime-id>' order by created_at;"
```

```bash
docker compose exec -T postgres psql -U agentic -d agentic \
  -c "select created_at, name, exec_id, parent_plan_name, graph_id, lifetime_id, status from task_executions where tenant_id='tenant-dev' and lifetime_id='<lifetime-id>' order by created_at;"
```

## 6) Graph status semantics

- Graph definition status (`agent_graphs.status`):
  - `NEW`: graph exists but has not been activated for execution
  - `ACTIVE`: graph is loaded and ready to run
  - `RUNNING`, `STOPPED`, `ERROR`: legacy states still represented in the model
- Graph run status (`graph_runs.status`, keyed by `lifetime_id`):
  - `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`
