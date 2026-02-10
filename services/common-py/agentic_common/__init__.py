"""
Common utilities for agentic microservices.

The package intentionally keeps imports lightweight so protobuf-only consumers
can import ``agentic_common.pb`` without pulling in optional runtime deps.
"""

from .pb_utils import ProtobufUtils

__all__ = ["ProtobufUtils"]
__version__ = "0.1.0"

try:
    from .kafka_utils import (
        get_persisted_plan_executions_topic,
        get_persisted_task_executions_topic,
        get_plan_execution_topic,
        get_task_execution_topic,
    )

    __all__.extend([
        "get_task_execution_topic",
        "get_plan_execution_topic",
        "get_persisted_task_executions_topic",
        "get_persisted_plan_executions_topic",
    ])
except Exception:
    # Optional dependency path (e.g., aiokafka) may be unavailable.
    pass

try:
    from .health import create_health_router
    from .logging_config import setup_logging

    __all__.extend(["create_health_router", "setup_logging"])
except Exception:
    # Optional dependency path (e.g., fastapi/structlog) may be unavailable.
    pass
