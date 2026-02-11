ALTER TABLE IF EXISTS agent_graphs
    DROP CONSTRAINT IF EXISTS agent_graphs_status_check;

ALTER TABLE IF EXISTS agent_graphs
    ADD CONSTRAINT agent_graphs_status_check
    CHECK (status IN ('NEW', 'ACTIVE', 'RUNNING', 'STOPPED', 'ERROR'));
