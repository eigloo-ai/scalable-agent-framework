# Runtime stage for Executor Java microservice.
# Expects JAR to be built on host before docker build.
FROM openjdk:21

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip curl && \
    rm -rf /var/lib/apt/lists/*

RUN pip3 install --no-cache-dir protobuf==6.31.1

RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash appuser

WORKDIR /app

COPY services/executor-java/target/executor-java-*.jar app.jar
COPY services/common-py /opt/agentic/common-py

RUN chown -R appuser:appgroup /app /opt/agentic

USER appuser

ENV EXECUTOR_PYTHON_COMMAND=python3
ENV EXECUTOR_PYTHON_COMMON_PY_PATH=/opt/agentic/common-py

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
