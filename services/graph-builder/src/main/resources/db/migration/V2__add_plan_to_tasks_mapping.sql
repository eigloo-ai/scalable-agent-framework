-- Add missing status column to agent_graphs table
ALTER TABLE agent_graphs ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'NEW';

-- Add index for status column
CREATE INDEX IF NOT EXISTS idx_agent_graph_status ON agent_graphs(status);

-- Add plan_to_tasks column to store the core graph structure as JSON
ALTER TABLE agent_graphs ADD COLUMN IF NOT EXISTS plan_to_tasks TEXT DEFAULT '{}';

-- Update existing records to have empty JSON object if null
UPDATE agent_graphs SET plan_to_tasks = '{}' WHERE plan_to_tasks IS NULL;