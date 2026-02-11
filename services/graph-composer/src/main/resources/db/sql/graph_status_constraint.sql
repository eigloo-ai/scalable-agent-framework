ALTER TABLE IF EXISTS agent_graphs
    DROP CONSTRAINT IF EXISTS agent_graphs_status_check;

UPDATE agent_graphs
SET status = 'ACTIVE'
WHERE status IN ('RUNNING', 'STOPPED', 'ERROR');

ALTER TABLE IF EXISTS agent_graphs
    ADD CONSTRAINT agent_graphs_status_check
    CHECK (status IN ('NEW', 'ACTIVE', 'ARCHIVED'));
