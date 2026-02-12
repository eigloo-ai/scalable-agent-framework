"""
Task base class for the agentic framework.

Tasks represent individual units of work that can be executed by the framework.
Each task takes the result of the previous plan and returns a TaskResult.
"""

import json
from abc import ABC, abstractmethod
from typing import Any, Dict, Optional, Union
from pathlib import Path

from agentic_common.pb import task_pb2
from google.protobuf import any_pb2
from agentic_common import ProtobufUtils

# PlanResult is imported as a string type annotation to avoid circular imports


class DeprecatedTaskExecutor(ABC):
    """
    DEPRECATED: Base class for all tasks in the agentic framework.
    
    This class is deprecated and will be removed in a future version.
    Use the new Task data class for graph structure instead.
    
    Tasks are individual units of work that can be executed. Each task
    takes the result of the previous plan and returns a TaskResult.
    """

    @abstractmethod
    async def execute(self, lastResult: "PlanResult") -> "TaskResult":
        """
        Execute the task based on the last plan result.

        Args:
            lastResult: Result from the previous plan execution

        Returns:
            TaskResult containing the execution output
        """
        pass


class Task:
    """
    Represents a task in the agent graph specification.
    
    A task is a node in the agent graph that processes plan results and produces
    a task result that can be consumed by downstream plans. Each task has a unique
    name and is associated with a Python subproject directory containing the task
    implementation.
    
    The task's Python subproject should contain:
    - A task.py file with a execute(upstream_plan: PlanResult) -> TaskResult function
    - A requirements.txt file listing dependencies
    
    Graph topology is represented by canonical edges outside this data object.
    """
    
    def __init__(self, name: str, label: str, task_source: Path):
        """
        Initialize a Task.
        
        Args:
            name: The unique identifier for this task
            label: The human-readable label for this task
            task_source: Path to the Python subproject directory containing the task implementation
            
        Raises:
            ValueError: If validation fails
        """
        if not name or not name.strip():
            raise ValueError("Task name cannot be null or empty")
        if task_source is None:
            raise ValueError("Task source cannot be null")
            
        self.name = name
        self.label = label
        self.task_source = task_source
    
    @classmethod
    def of(cls, name: str, task_source: Path) -> "Task":
        """
        Creates a new Task with the specified name and source directory.
        
        Args:
            name: The task name
            task_source: The task source directory
            
        Returns:
            A new Task
        """
        return cls(name, name, task_source)
    
    def __eq__(self, other):
        if not isinstance(other, Task):
            return False
        return (self.name == other.name and 
                self.label == other.label and 
                self.task_source == other.task_source)
    
    def __hash__(self):
        return hash((self.name, self.label, self.task_source))
    
    def __repr__(self):
        return f"Task(name='{self.name}', label='{self.label}', task_source={self.task_source})"


class TaskResult:
    """
    Result of a task execution.

    Contains either inline data or a URI reference to external storage,
    along with metadata about the result.
    """

    def __init__(
        self,
        data: Optional[Union[Any, str]] = None,
        error_message: Optional[str] = None
    ):
        """
        Initialize a TaskResult.

        Args:
            data: Result data (inline object or URI string)
            error_message: Optional error message if execution failed
        """
        self.data = data
        self.error_message = error_message

    def to_protobuf(self) -> "task_pb2.TaskResult":
        """
        Convert this result to a TaskResult protobuf message.

        Returns:
            TaskResult protobuf message
        """
        # Use consistent protobuf utilities for creation
        return ProtobufUtils.create_task_result(
            error_message=self.error_message or "",
            inline_data=self.data if not isinstance(self.data, str) else None,
            external_data_uri=self.data if isinstance(self.data, str) else ""
        )

    @classmethod
    def from_protobuf(cls, proto: "task_pb2.TaskResult") -> "TaskResult":
        """
        Create a TaskResult from a protobuf message.

        Args:
            proto: TaskResult protobuf message

        Returns:
            TaskResult instance
        """
        data = None
        if proto.HasField("inline_data"):
            data = proto.inline_data
        elif proto.HasField("external_data"):
            data = proto.external_data.uri

        return cls(
            data=data,
            error_message=proto.error_message if proto.error_message else None
        )
