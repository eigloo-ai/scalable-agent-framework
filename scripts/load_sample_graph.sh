#!/usr/bin/env bash
set -euo pipefail

# Seeds a runnable sample graph into PostgreSQL:
#   PlanA -> Task1A, Task1B
#   Task1A -> PlanB
#   PlanB -> Task2
#
# Required tools:
#   - psql
#
# Environment variables (optional):
#   TENANT_ID   (default: tenant-dev)
#   GRAPH_NAME  (default: sample-graph-plan-a-plan-b)
#   PGHOST      (default: localhost)
#   PGPORT      (default: 5432)
#   PGDATABASE  (default: agentic)
#   PGUSER      (default: agentic)
#   PGPASSWORD  (optional; used by psql if auth requires a password)

TENANT_ID="${TENANT_ID:-tenant-dev}"
GRAPH_NAME="${GRAPH_NAME:-sample-graph-plan-a-plan-b}"
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-agentic}"
PGUSER="${PGUSER:-agentic}"

if ! command -v psql >/dev/null 2>&1; then
    echo "error: psql is required but was not found in PATH" >&2
    exit 1
fi

echo "Seeding sample graph '${GRAPH_NAME}' for tenant '${TENANT_ID}' into ${PGUSER}@${PGHOST}:${PGPORT}/${PGDATABASE}"

psql \
  --host "${PGHOST}" \
  --port "${PGPORT}" \
  --username "${PGUSER}" \
  --dbname "${PGDATABASE}" \
  --set ON_ERROR_STOP=1 \
  --set tenant_id="${TENANT_ID}" \
  --set graph_name="${GRAPH_NAME}" <<'SQL'
BEGIN;

CREATE OR REPLACE FUNCTION pg_temp.seed_uuid(seed TEXT) RETURNS UUID
LANGUAGE SQL
IMMUTABLE
STRICT
AS $$
    SELECT (
        substr(md5(seed), 1, 8) || '-' ||
        substr(md5(seed), 9, 4) || '-' ||
        substr(md5(seed), 13, 4) || '-' ||
        substr(md5(seed), 17, 4) || '-' ||
        substr(md5(seed), 21, 12)
    )::uuid;
$$;

-- Backfill minimal schema pieces needed for executor-java path.
ALTER TABLE agent_graphs ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'NEW';
ALTER TABLE agent_graphs ADD COLUMN IF NOT EXISTS plan_to_tasks TEXT DEFAULT '{}';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS downstream_plan_id VARCHAR(36);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_tasks_downstream_plan'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_tasks_downstream_plan
            FOREIGN KEY (downstream_plan_id)
            REFERENCES plans(id)
            ON DELETE SET NULL;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_task_downstream_plan ON tasks(downstream_plan_id);

CREATE TABLE IF NOT EXISTS executor_files (
    id            VARCHAR(36) PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    contents      TEXT,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version       VARCHAR(50),
    update_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    plan_id       VARCHAR(36),
    task_id       VARCHAR(36),
    CONSTRAINT fk_executor_file_plan
        FOREIGN KEY (plan_id)
        REFERENCES plans(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_executor_file_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_executor_file_plan_id ON executor_files(plan_id);
CREATE INDEX IF NOT EXISTS idx_executor_file_task_id ON executor_files(task_id);
CREATE INDEX IF NOT EXISTS idx_executor_file_name ON executor_files(file_name);

CREATE TEMP TABLE seed_ids AS
SELECT
    pg_temp.seed_uuid('graph|' || :'tenant_id' || '|' || :'graph_name')::text AS graph_id,
    pg_temp.seed_uuid('plan|PlanA|' || :'tenant_id' || '|' || :'graph_name')::text AS plan_a_id,
    pg_temp.seed_uuid('plan|PlanB|' || :'tenant_id' || '|' || :'graph_name')::text AS plan_b_id,
    pg_temp.seed_uuid('task|Task1A|' || :'tenant_id' || '|' || :'graph_name')::text AS task_1a_id,
    pg_temp.seed_uuid('task|Task1B|' || :'tenant_id' || '|' || :'graph_name')::text AS task_1b_id,
    pg_temp.seed_uuid('task|Task2|' || :'tenant_id' || '|' || :'graph_name')::text AS task_2_id;

-- Remove any prior graph with same tenant/name but a different deterministic ID.
DELETE FROM executor_files ef
USING plans p, agent_graphs g
WHERE ef.plan_id = p.id
  AND p.graph_id = g.id
  AND g.tenant_id = :'tenant_id'
  AND g.name = :'graph_name'
  AND g.id <> (SELECT graph_id FROM seed_ids);

DELETE FROM executor_files ef
USING tasks t, agent_graphs g
WHERE ef.task_id = t.id
  AND t.graph_id = g.id
  AND g.tenant_id = :'tenant_id'
  AND g.name = :'graph_name'
  AND g.id <> (SELECT graph_id FROM seed_ids);

DELETE FROM agent_graphs g
WHERE g.tenant_id = :'tenant_id'
  AND g.name = :'graph_name'
  AND g.id <> (SELECT graph_id FROM seed_ids);

-- Clear deterministic graph rows before reseeding.
DELETE FROM executor_files
WHERE plan_id IN (
    SELECT p.id
    FROM plans p
    WHERE p.graph_id = (SELECT graph_id FROM seed_ids)
);

DELETE FROM executor_files
WHERE task_id IN (
    SELECT t.id
    FROM tasks t
    WHERE t.graph_id = (SELECT graph_id FROM seed_ids)
);

DELETE FROM tasks
WHERE graph_id = (SELECT graph_id FROM seed_ids);

DELETE FROM plans
WHERE graph_id = (SELECT graph_id FROM seed_ids);

INSERT INTO agent_graphs (id, tenant_id, name, status, plan_to_tasks, created_at, updated_at)
SELECT
    graph_id,
    :'tenant_id',
    :'graph_name',
    'RUNNING',
    '{"PlanA":["Task1A","Task1B"],"PlanB":["Task2"]}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM seed_ids
ON CONFLICT (id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    name = EXCLUDED.name,
    status = EXCLUDED.status,
    plan_to_tasks = EXCLUDED.plan_to_tasks,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO plans (id, name, label, plan_source_path, graph_id)
SELECT
    plan_a_id,
    'PlanA',
    'Plan A',
    'plans/PlanA/plan.py',
    graph_id
FROM seed_ids
UNION ALL
SELECT
    plan_b_id,
    'PlanB',
    'Plan B',
    'plans/PlanB/plan.py',
    graph_id
FROM seed_ids;

INSERT INTO tasks (id, name, label, task_source, graph_id, upstream_plan_id, downstream_plan_id)
SELECT
    task_1a_id,
    'Task1A',
    'Task 1A',
    'tasks/Task1A/task.py',
    graph_id,
    plan_a_id,
    plan_b_id
FROM seed_ids
UNION ALL
SELECT
    task_1b_id,
    'Task1B',
    'Task 1B',
    'tasks/Task1B/task.py',
    graph_id,
    plan_a_id,
    NULL
FROM seed_ids
UNION ALL
SELECT
    task_2_id,
    'Task2',
    'Task 2',
    'tasks/Task2/task.py',
    graph_id,
    plan_b_id,
    NULL
FROM seed_ids;

DO $$
BEGIN
    IF to_regclass('public.plan_upstream_tasks') IS NOT NULL THEN
        DELETE FROM plan_upstream_tasks
        WHERE plan_id IN (
            SELECT plan_a_id FROM seed_ids
            UNION ALL
            SELECT plan_b_id FROM seed_ids
        );

        INSERT INTO plan_upstream_tasks (plan_id, task_id)
        VALUES (
            (SELECT plan_b_id FROM seed_ids),
            (SELECT task_1a_id FROM seed_ids)
        )
        ON CONFLICT DO NOTHING;
    END IF;
END
$$;

INSERT INTO executor_files (id, file_name, contents, creation_date, version, update_date, plan_id, task_id)
VALUES
(
    pg_temp.seed_uuid('file|PlanA|plan.py|' || :'tenant_id' || '|' || :'graph_name')::text,
    'plan.py',
    $plan_a$
from agentic_common.pb import PlanInput, PlanResult


def plan(plan_input: PlanInput) -> PlanResult:
    result = PlanResult()
    result.next_task_names.extend(["Task1A", "Task1B"])
    return result
$plan_a$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    (SELECT plan_a_id FROM seed_ids),
    NULL
),
(
    pg_temp.seed_uuid('file|PlanA|requirements.txt|' || :'tenant_id' || '|' || :'graph_name')::text,
    'requirements.txt',
    $plan_a_reqs$
# Dependencies for PlanA
$plan_a_reqs$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    (SELECT plan_a_id FROM seed_ids),
    NULL
),
(
    pg_temp.seed_uuid('file|PlanB|plan.py|' || :'tenant_id' || '|' || :'graph_name')::text,
    'plan.py',
    $plan_b$
from agentic_common.pb import PlanInput, PlanResult


def plan(plan_input: PlanInput) -> PlanResult:
    result = PlanResult()
    result.next_task_names.append("Task2")
    return result
$plan_b$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    (SELECT plan_b_id FROM seed_ids),
    NULL
),
(
    pg_temp.seed_uuid('file|PlanB|requirements.txt|' || :'tenant_id' || '|' || :'graph_name')::text,
    'requirements.txt',
    $plan_b_reqs$
# Dependencies for PlanB
$plan_b_reqs$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    (SELECT plan_b_id FROM seed_ids),
    NULL
),
(
    pg_temp.seed_uuid('file|Task1A|task.py|' || :'tenant_id' || '|' || :'graph_name')::text,
    'task.py',
    $task_1a$
from agentic_common.pb import TaskInput, TaskResult


def task(task_input: TaskInput) -> TaskResult:
    result = TaskResult()
    result.id = task_input.input_id or "Task1A-result"
    return result
$task_1a$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    NULL,
    (SELECT task_1a_id FROM seed_ids)
),
(
    pg_temp.seed_uuid('file|Task1A|requirements.txt|' || :'tenant_id' || '|' || :'graph_name')::text,
    'requirements.txt',
    $task_1a_reqs$
# Dependencies for Task1A
$task_1a_reqs$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    NULL,
    (SELECT task_1a_id FROM seed_ids)
),
(
    pg_temp.seed_uuid('file|Task1B|task.py|' || :'tenant_id' || '|' || :'graph_name')::text,
    'task.py',
    $task_1b$
from agentic_common.pb import TaskInput, TaskResult


def task(task_input: TaskInput) -> TaskResult:
    result = TaskResult()
    result.id = task_input.input_id or "Task1B-result"
    return result
$task_1b$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    NULL,
    (SELECT task_1b_id FROM seed_ids)
),
(
    pg_temp.seed_uuid('file|Task1B|requirements.txt|' || :'tenant_id' || '|' || :'graph_name')::text,
    'requirements.txt',
    $task_1b_reqs$
# Dependencies for Task1B
$task_1b_reqs$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    NULL,
    (SELECT task_1b_id FROM seed_ids)
),
(
    pg_temp.seed_uuid('file|Task2|task.py|' || :'tenant_id' || '|' || :'graph_name')::text,
    'task.py',
    $task_2$
from agentic_common.pb import TaskInput, TaskResult


def task(task_input: TaskInput) -> TaskResult:
    result = TaskResult()
    result.id = task_input.input_id or "Task2-result"
    return result
$task_2$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    NULL,
    (SELECT task_2_id FROM seed_ids)
),
(
    pg_temp.seed_uuid('file|Task2|requirements.txt|' || :'tenant_id' || '|' || :'graph_name')::text,
    'requirements.txt',
    $task_2_reqs$
# Dependencies for Task2
$task_2_reqs$,
    CURRENT_TIMESTAMP,
    '1.0',
    CURRENT_TIMESTAMP,
    NULL,
    (SELECT task_2_id FROM seed_ids)
)
ON CONFLICT (id) DO UPDATE SET
    file_name = EXCLUDED.file_name,
    contents = EXCLUDED.contents,
    version = EXCLUDED.version,
    update_date = CURRENT_TIMESTAMP,
    plan_id = EXCLUDED.plan_id,
    task_id = EXCLUDED.task_id;

SELECT
    (SELECT graph_id FROM seed_ids) AS graph_id,
    (SELECT count(*) FROM plans WHERE graph_id = (SELECT graph_id FROM seed_ids)) AS plans_seeded,
    (SELECT count(*) FROM tasks WHERE graph_id = (SELECT graph_id FROM seed_ids)) AS tasks_seeded,
    (
        SELECT count(*)
        FROM executor_files ef
        WHERE ef.plan_id IN (SELECT id FROM plans WHERE graph_id = (SELECT graph_id FROM seed_ids))
           OR ef.task_id IN (SELECT id FROM tasks WHERE graph_id = (SELECT graph_id FROM seed_ids))
    ) AS executor_files_seeded;

COMMIT;
SQL

echo "Sample graph seed complete."
