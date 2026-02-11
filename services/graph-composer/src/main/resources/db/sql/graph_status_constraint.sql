ALTER TABLE IF EXISTS agent_graphs
    DROP CONSTRAINT IF EXISTS agent_graphs_status_check;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'agent_graphs') THEN
        UPDATE agent_graphs
        SET status = 'ACTIVE'
        WHERE status IN ('RUNNING', 'STOPPED', 'ERROR');
    END IF;
END
$$;

ALTER TABLE IF EXISTS agent_graphs
    ADD CONSTRAINT agent_graphs_status_check
    CHECK (status IN ('NEW', 'ACTIVE', 'ARCHIVED'));
