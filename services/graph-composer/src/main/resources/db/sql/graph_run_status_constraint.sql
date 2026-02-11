ALTER TABLE IF EXISTS graph_runs
    DROP CONSTRAINT IF EXISTS graph_runs_status_check;

ALTER TABLE IF EXISTS graph_runs
    ADD CONSTRAINT graph_runs_status_check
    CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED'));
