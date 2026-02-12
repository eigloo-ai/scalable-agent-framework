## Build

```bash
mvn clean package
```

## Startup

```bash
docker compose -f docker-compose.yml up -d --build kafka postgres graph-composer executor-java data-plane control-plane
```


## Health
```bash
curl -fsS http://localhost:8088/actuator/health
curl -fsS http://localhost:8081/actuator/health
curl -fsS http://localhost:8082/actuator/health
curl -fsS http://localhost:8083/actuator/health
```

## Seed graph
```bash
./scripts/load_sample_graph.sh \
  --base-url http://localhost:8088 \
  --tenant-id tenant-dev \
  --graph-name sample-graph-$(date +%s) \
  --execute
```


