# Runtime stage for Executor Java microservice.
# Expects JAR to be built on host before docker build.
FROM eclipse-temurin:21-jre

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip python3-venv curl && \
    rm -rf /var/lib/apt/lists/*

RUN python3 -m venv /opt/agentic/venv && \
    /opt/agentic/venv/bin/pip install --no-cache-dir protobuf==6.33.4

RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash appuser

WORKDIR /app

COPY services/executor-java/target/executor-java-*-exec.jar app.jar
COPY services/common-py /opt/agentic/common-py

RUN chown -R appuser:appgroup /app /opt/agentic

USER appuser

ENV EXECUTOR_PYTHON_COMMAND=/opt/agentic/venv/bin/python
ENV EXECUTOR_PYTHON_COMMON_PY_PATH=/opt/agentic/common-py

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
