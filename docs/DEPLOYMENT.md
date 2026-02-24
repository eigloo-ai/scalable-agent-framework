# Deployment Guide

This guide explains how to deploy the Scalable Agent Framework using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Java 21+ (for building services)
- Maven 3.9+ (for building services)
- At least 4GB RAM available for containers
- At least 10GB disk space

## Quick Start

### 1. Build All Services

```bash
# Build all Java microservices
make java-build

# Or build individual services
make common-java-build
make control-plane-build
make data-plane-build
```

### 2. Start Local Development Environment

```bash
# Start all services with Kafka and PostgreSQL
make java-up

# Or directly with docker-compose
docker-compose up -d

# View logs
make java-logs

# Stop services
make java-down
```

### 3. Access Services

| Service | URL | Health Check |
|---------|-----|-------------|
| **Data Plane** | http://localhost:8081 | `GET /actuator/health` |
| **Control Plane** | http://localhost:8082 | `GET /actuator/health` |
| **Executor** | http://localhost:8083 | `GET /actuator/health` |
| **pgAdmin** | http://localhost:8085 | - |
| **Admin** | http://localhost:8086 | `GET /actuator/health` |
| **Graph Builder** | http://localhost:8087 | `GET /actuator/health` |
| **Graph Composer** | http://localhost:8088 | `GET /actuator/health` |
| **Kafka UI** | http://localhost:8080 | - |
| **Frontend** | http://localhost:5173 | - |
| **PostgreSQL** | localhost:5432 | - |

## Service Architecture

All services are Java 21 / Spring Boot applications communicating via Kafka with Protocol Buffers serialization.

### Data Plane (Port 8081)
- **Purpose**: Persists execution records (append-only) and forwards messages to control plane topics
- **Database**: PostgreSQL (Flyway migrations)
- **Health Check**: `GET /actuator/health`
- **Kafka Topics Consumed**: `task-executions-{tenantId}`, `plan-executions-{tenantId}`
- **Kafka Topics Produced**: `persisted-task-executions-{tenantId}`, `persisted-plan-executions-{tenantId}`

### Control Plane (Port 8082)
- **Purpose**: Evaluates guardrails and routes execution messages to the next graph node
- **Health Check**: `GET /actuator/health`
- **Kafka Topics Consumed**: `persisted-task-executions-{tenantId}`, `persisted-plan-executions-{tenantId}`
- **Kafka Topics Produced**: `plan-inputs-{tenantId}`, `task-inputs-{tenantId}`

### Executor (Port 8083)
- **Purpose**: Executes task and plan Python code via subprocess
- **Health Check**: `GET /actuator/health`
- **Kafka Topics Consumed**: `plan-inputs-{tenantId}`, `task-inputs-{tenantId}`
- **Kafka Topics Produced**: `plan-executions-{tenantId}`, `task-executions-{tenantId}`

### Graph Composer (Port 8088)
- **Purpose**: Web UI and REST API for creating, editing, and visualizing agent graphs
- **Health Check**: `GET /actuator/health`

### Infrastructure Services

#### Kafka (Port 9092)
- **Purpose**: Message broker for inter-service communication
- **Mode**: KRaft (no Zookeeper required)
- **Topics**: 6 topic types with tenant-aware naming `{type}-{tenantId}`
- **Management**: Kafka UI at http://localhost:8080

#### PostgreSQL (Port 5432)
- **Purpose**: Persistent data storage
- **Database**: agentic
- **Credentials**: agentic/agentic

## Configuration

### Environment Variables

All services use Spring Boot configuration. Key environment variables:

#### Spring Boot Common
- `SPRING_PROFILES_ACTIVE`: Active profile (default: `docker` in containers)
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `SPRING_KAFKA_CONSUMER_AUTO_OFFSET_RESET`: Consumer offset reset strategy
- `SPRING_KAFKA_CONSUMER_ENABLE_AUTO_COMMIT`: Auto-commit toggle (set to `false`)

#### Database (Data Plane, Control Plane, Graph Services)
- `SPRING_DATASOURCE_URL`: JDBC connection string (e.g., `jdbc:postgresql://postgres:5432/agentic`)
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

#### Executor
- `EXECUTOR_PYTHON_COMMAND`: Path to Python binary in container
- `EXECUTOR_PYTHON_COMMON_PY_PATH`: Path to shared Python library

### Configuration Files

Each service has its own `application.yml` in `services/{service}/src/main/resources/`:
- Kafka topic patterns and consumer group configuration
- Database connection and Flyway migration settings
- Logging levels

## Development Workflow

### 1. Local Development

```bash
# Start development environment
make java-up

# View specific service logs
docker-compose logs -f control-plane

# Rebuild and restart a single service (e.g., control-plane)
make control-plane-docker-up

# Run the sample graph end-to-end
./scripts/load_sample_graph.sh \
  --base-url http://localhost:8088 \
  --tenant-id tenant-dev \
  --graph-name sample-graph-plan-a-plan-b \
  --execute
```

### 2. Testing

```bash
# Run Java unit tests for a specific module
cd services/common-java && mvn test

# Run integration health checks
make java-test
```

### 3. Debugging

```bash
# Access service container
docker-compose exec control-plane bash

# View service logs
docker-compose logs -f executor-java

# Check service health
curl http://localhost:8082/actuator/health

# Query execution timeline
curl "http://localhost:8081/api/v1/runs/{lifetime-id}/timeline?tenantId=tenant-dev&graphId={graph-id}"

# Inspect PostgreSQL directly
docker-compose exec -T postgres psql -U agentic -d agentic \
  -c "SELECT * FROM plan_executions WHERE tenant_id='tenant-dev' ORDER BY created_at;"
```

## Production Deployment

### 1. Environment-Specific Configuration

Create environment-specific override files:

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  control-plane:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka-prod:9092
    restart: unless-stopped
```

### 2. Security Considerations

- Use secrets management for database credentials
- Enable TLS for inter-service communication
- Configure proper network policies
- Use non-root users in containers

### 3. Monitoring

- Spring Boot Actuator endpoints expose health and metrics
- Configure log aggregation (all services output structured logs)
- Monitor Kafka consumer lag via Kafka UI or JMX
- Set up alerting on health check failures

### 4. Scaling

```bash
# Scale executor instances
docker-compose up -d --scale executor-java=3
```

## Troubleshooting

### Common Issues

#### 1. Service Won't Start
```bash
# Check service logs
docker-compose logs control-plane

# Check service health
curl http://localhost:8082/actuator/health

# Verify dependencies are healthy
docker-compose ps
```

#### 2. Kafka Connection Issues
```bash
# Check Kafka status
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check Kafka UI
open http://localhost:8080
```

#### 3. Database Connection Issues
```bash
# Check PostgreSQL
docker-compose exec postgres psql -U agentic -d agentic

# Check Flyway migration status
curl http://localhost:8081/actuator/flyway
```

### Log Analysis

```bash
# View all logs
docker-compose logs

# Filter logs by service
docker-compose logs control-plane | grep ERROR

# Follow logs in real-time
docker-compose logs -f --tail=100
```

## Backup and Recovery

### Database Backup

```bash
# Create backup
docker-compose exec postgres pg_dump -U agentic agentic > backup.sql

# Restore backup
docker-compose exec -T postgres psql -U agentic agentic < backup.sql
```
