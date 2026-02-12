-- Flyway baseline schema for graph_builder-java (PostgreSQL-compatible)

-- graph_bundles
CREATE TABLE IF NOT EXISTS graph_bundles (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(100)  NOT NULL,
    graph_name      VARCHAR(255),
    file_name       VARCHAR(255)  NOT NULL,
    status          VARCHAR(50)   NOT NULL,
    upload_time     TIMESTAMP     NOT NULL,
    completion_time TIMESTAMP,
    error_message   TEXT,
    process_id      VARCHAR(36)   NOT NULL,
    CONSTRAINT uq_graph_bundles_process_id UNIQUE (process_id)
);

CREATE INDEX IF NOT EXISTS idx_graph_bundle_tenant_id ON graph_bundles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_graph_bundle_status ON graph_bundles(status);
CREATE INDEX IF NOT EXISTS idx_graph_bundle_upload_time ON graph_bundles(upload_time);

-- processing_steps
CREATE TABLE IF NOT EXISTS processing_steps (
    id            VARCHAR(36) PRIMARY KEY,
    process_id    VARCHAR(36)  NOT NULL,
    step_name     VARCHAR(100) NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    start_time    TIMESTAMP    NOT NULL,
    end_time      TIMESTAMP,
    error_message TEXT,
    CONSTRAINT fk_processing_steps_process
        FOREIGN KEY (process_id)
        REFERENCES graph_bundles(process_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_processing_step_process_id ON processing_steps(process_id);
CREATE INDEX IF NOT EXISTS idx_processing_step_status ON processing_steps(status);
CREATE INDEX IF NOT EXISTS idx_processing_step_start_time ON processing_steps(start_time);

-- docker_images
CREATE TABLE IF NOT EXISTS docker_images (
    id             VARCHAR(36) PRIMARY KEY,
    process_id     VARCHAR(36)  NOT NULL,
    image_name     VARCHAR(255) NOT NULL,
    image_tag      VARCHAR(100),
    executor_type  VARCHAR(20)  NOT NULL,
    executor_name  VARCHAR(100) NOT NULL,
    build_time     TIMESTAMP    NOT NULL,
    CONSTRAINT fk_docker_images_process
        FOREIGN KEY (process_id)
        REFERENCES graph_bundles(process_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_docker_image_process_id ON docker_images(process_id);
CREATE INDEX IF NOT EXISTS idx_docker_image_executor_type ON docker_images(executor_type);
CREATE INDEX IF NOT EXISTS idx_docker_image_executor_name ON docker_images(executor_name);
CREATE INDEX IF NOT EXISTS idx_docker_image_build_time ON docker_images(build_time);

-- agent_graphs
CREATE TABLE IF NOT EXISTS agent_graphs (
    id          VARCHAR(36)  PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_agent_graph_tenant_id ON agent_graphs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_graph_name ON agent_graphs(name);
CREATE INDEX IF NOT EXISTS idx_agent_graph_tenant_name ON agent_graphs(tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_agent_graph_status ON agent_graphs(status);

-- plans
CREATE TABLE IF NOT EXISTS plans (
    id               VARCHAR(36)  PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    label            VARCHAR(255),
    plan_source_path VARCHAR(500),
    graph_id         VARCHAR(36)  NOT NULL,
    CONSTRAINT fk_plans_graph
        FOREIGN KEY (graph_id)
        REFERENCES agent_graphs(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_plan_graph_id ON plans(graph_id);
CREATE INDEX IF NOT EXISTS idx_plan_name ON plans(name);
CREATE INDEX IF NOT EXISTS idx_plan_graph_name ON plans(graph_id, name);

-- tasks
CREATE TABLE IF NOT EXISTS tasks (
    id               VARCHAR(36)  PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    label            VARCHAR(255),
    task_source      VARCHAR(500),
    graph_id         VARCHAR(36)  NOT NULL,
    CONSTRAINT fk_tasks_graph
        FOREIGN KEY (graph_id)
        REFERENCES agent_graphs(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_task_graph_id ON tasks(graph_id);
CREATE INDEX IF NOT EXISTS idx_task_name ON tasks(name);
CREATE INDEX IF NOT EXISTS idx_task_graph_name ON tasks(graph_id, name);

-- canonical directed edges for graph topology
CREATE TABLE IF NOT EXISTS graph_edges (
    id             VARCHAR(36)  PRIMARY KEY,
    graph_id       VARCHAR(36)  NOT NULL,
    from_node_name VARCHAR(255) NOT NULL,
    from_node_type VARCHAR(20)  NOT NULL,
    to_node_name   VARCHAR(255) NOT NULL,
    to_node_type   VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_graph_edges_graph
        FOREIGN KEY (graph_id)
        REFERENCES agent_graphs(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_graph_edges_types
        CHECK (
            (from_node_type = 'PLAN' AND to_node_type = 'TASK')
            OR
            (from_node_type = 'TASK' AND to_node_type = 'PLAN')
        )
);

CREATE INDEX IF NOT EXISTS idx_graph_edges_graph_id ON graph_edges(graph_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_from ON graph_edges(graph_id, from_node_type, from_node_name);
CREATE INDEX IF NOT EXISTS idx_graph_edges_to ON graph_edges(graph_id, to_node_type, to_node_name);
CREATE UNIQUE INDEX IF NOT EXISTS uq_graph_edges_unique
    ON graph_edges(graph_id, from_node_name, from_node_type, to_node_name, to_node_type);
