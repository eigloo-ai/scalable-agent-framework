# Agent Graph Specification System

## Overview

The Agent Graph Specification System allows defining dynamic agent graphs using a graph specification file and a bundle of Python code for each node. This system provides a declarative approach to defining agent graphs, enabling easy configuration and dynamic execution.

### Features

- **Declarative Configuration**: Define agent graphs using standard syntax
- **Validation**: Comprehensive validation ensures graph integrity and correctness
- **Extensible**: Pluggable parser architecture supports multiple specification formats
- **Type Safety**: Strong typing with Java records ensures data integrity

## Message Flow Architecture

The system implements a sophisticated message flow architecture that ensures proper data transformation and routing between services:

### ControlPlane Message Conversion

The ControlPlane service plays a crucial role in the message flow by examining TaskExecution messages and looking up the next Plan in the graph:

1. **TaskExecution Reception**: ControlPlane receives TaskExecution protobuf messages from DataPlane via `persisted-task-executions-{tenantId}` topics
2. **Guardrail Evaluation**: ControlPlane evaluates guardrails and policies on the TaskExecution data
3. **Next Plan Prep**: ControlPlane examines the TaskExecution and looks up the next Plan in the graph
4. **PlanInput Publication**: ControlPlane publishes the PlanInput to `plan-inputs-{tenantId}` topics for `executor-java` plan-input consumption

### PlanInput Message Structure

The PlanInput protobuf message contains:
- **input_id**: Unique identifier for this input for auditing
- **plan_name**: Name of the next plan in the graph path
- **task_executions**: List of TaskExecution objects that provide input to this plan

This process ensures that `executor-java` receives properly structured plan input data with the correct next-plan information from the graph.
## Directory Structure

A complete agent graph specification consists of the following directory structure:

```
specification_directory/
├── agent_graph.dot          # Graph specification file (GraphViz DOT format)
├── plans/                   # Python subprojects for plans
│   ├── plan_name1/
│   │   ├── plan.py         # Plan implementation
│   │   └── requirements.txt # Plan dependencies
│   └── plan_name2/
│       ├── plan.py
│       └── requirements.txt
└── tasks/                   # Python subprojects for tasks
    ├── task_name1/
    │   ├── task.py         # Task implementation
    │   └── requirements.txt # Task dependencies
    └── task_name2/
        ├── task.py
        └── requirements.txt
```

### Required Files

#### Graph Specification File
- **Format**: Currently supports GraphViz DOT (`.dot` extension)
- **Location**: Root of specification directory
- **Purpose**: Defines nodes (plans/tasks) and edges (data flow)
- **Naming**: Must be a single file named with `.dot` extension in root directory.  (Convention: `agent_graph.dot`)

#### Plan Implementation
- **File**: `plan.py` in each plan subdirectory
- **Function**: `plan(upstream_results: List[TaskResult]) -> PlanResult`
- **Purpose**: Process results from upstream tasks and produce plan results
- **Directory**: Must be in `plans/` directory with subdirectory matching node name

#### Task Implementation
- **File**: `task.py` in each task subdirectory
- **Function**: `execute(upstream_plan: PlanResult) -> TaskResult`
- **Purpose**: Process results from upstream plans and produce task results
- **Directory**: Must be in `tasks/` directory with subdirectory matching node name

#### Dependencies
- **File**: `requirements.txt` in each subdirectory
- **Format**: Standard Python requirements format
- **Purpose**: Define Python dependencies for each plan/task
- **Required**: Every plan and task subdirectory must have this file

## Graph Specification Format

### GraphViz DOT Syntax

The graph specification uses standard GraphViz DOT syntax with custom attributes to distinguish between plans and tasks:

```dot
digraph AgentGraph {
    // Graph attributes
    rankdir=TB;
    node [shape=box, style=filled];
    
    // Plan nodes (process task results)
    plan_data_collection [label="Data Collection Plan", type="plan"];
    plan_analysis [label="Analysis Plan", type="plan"];
    
    // Task nodes (process plan results)
    task_fetch_data [label="Fetch Data", type="task"];
    task_process_data [label="Process Data", type="task"];
    task_generate_report [label="Generate Report", type="task"];
    
    // Edges showing data flow
    // Plans -> Tasks
    plan_data_collection -> task_fetch_data;
    plan_data_collection -> task_process_data;
    plan_analysis -> task_generate_report;
}
```

### Node Identification

Nodes are identified as plans or tasks using **Node Attributes**: Use `type="plan"` or `type="task"` attributes

### Edge Semantics

- **Plan → Task**: Plan results feed into task execution
- **Task → Plan**: Task results feed into plan execution
- **Constraints**: Tasks can only have one upstream plan, plans can have multiple upstream tasks

## Plan and Task Requirements

### Plan Implementation

Each plan must implement a `plan.py` file with the following signature:

```python
from typing import List

def plan(upstream_results: List[TaskResult]) -> PlanResult:
    """
    Process results from upstream tasks and produce a plan result.
    
    Args:
        upstream_results: List of results from upstream tasks
        
    Returns:
        PlanResult: The plan result that will be consumed by downstream tasks
    """
    # Implementation here
    return PlanResult(data=processed_data)
```

### Task Implementation

Each task must implement a `task.py` file with the following signature:

```python
def execute(upstream_plan: PlanResult) -> TaskResult:
    """
    Process results from upstream plan and produce a task result.
    
    Args:
        upstream_plan: Result from the upstream plan
        
    Returns:
        TaskResult: The task result that will be consumed by downstream plans
    """
    # Implementation here
    return TaskResult(data=processed_data)
```

### Task and Plan Dependencies

Each plan and task subdirectory must contain a `requirements.txt` file:

```
# Example requirements.txt for a plan
requests>=2.25.0
pandas>=1.3.0
numpy>=1.20.0

# Example requirements.txt for a task
pandas>=1.3.0
scikit-learn>=1.0.0
```

## Validation Rules

The system enforces the following integrity constraints:

### Graph Structure
- **Unique Names**: All plan and task names must be unique across the entire graph
- **Valid Identifiers**: Node names must be valid Python identifiers (letters, digits, underscores)
- **Connectivity**: All nodes must be connected (no orphaned nodes)
- **Cycles Allowed**: The graph can contain cycles (circular dependencies are permitted)

### Edge Constraints
- **Dangling References**: All edges must reference existing nodes
- **Task Single Upstream**: Each task can only have one upstream plan
- **Plan Multiple Upstream**: Plans can have multiple upstream tasks
- **Valid Flow**: Edges must connect plans to tasks or tasks to plans

### Plan and Task Constraints
- **Plan Feeds Into Tasks**: Every plan must feed into one or more tasks
- **Task Has Upstream Plan**: Every task must have an upstream/parent plan

### Directory Structure
- **Required Directories**: `plans/` and `tasks/` directories must exist
- **Python Files**: Each plan must have `plan.py`, each task must have `task.py`
- **Requirements**: Each subdirectory must have `requirements.txt`
- **Directory Names**: Plan and task subdirectory names must exactly match the node names in the DOT file

### File Validation
- **Python Syntax**: All Python files must have valid syntax
- **Function Signatures**: Functions must match expected signatures
- **Dependencies**: Requirements files must be valid Python requirements format

## Parser Extension

The system uses a pluggable parser architecture that allows adding support for new specification formats.


### Supported Formats

Currently supported formats:
- **GraphViz DOT**: `.dot` files
- **Future**: JSON, YAML, mermaid formats



### GraphParsingException

Thrown when the specification file cannot be parsed:

- **Malformed DOT syntax**
- **Missing specification file**
- **Invalid file format**
- **I/O errors**


### GraphValidationException

Thrown when the graph violates integrity constraints:

- **Duplicate node names**
- **Dangling edge references**
- **Task multiple upstream plans**
- **Orphaned nodes**
- **Invalid node names**
- **Missing directories/files**
- **Plans not feeding into tasks**
- **Tasks without upstream plans**

### Common Error Scenarios

1. **Missing Python Files**:
   ```
   Validation error: Plan directory does not exist: /path/plans/plan1
   ```

2. **Dangling Edge**:
   ```
   Validation error: Plan plan1 references non-existent task: nonexistent_task
   ```

3. **Task Multiple Plans**:
   ```
   Validation error: Task task1 upstream plan mismatch: task says plan1, graph says plan2
   ```

4. **Invalid Node Name**:
   ```
   Validation error: Invalid plan name: invalid-plan-name (must be a valid identifier)
   ```

5. **Plan Not Feeding Into Tasks**:
   ```
   Validation error: Plan plan1 does not feed into any tasks
   ```

6. **Task Without Upstream Plan**:
   ```
   Validation error: Task task1 has no upstream plan
   ```

## Testing

The system includes comprehensive test infrastructure with example graphs located in `src/test/resources/valid_graphs/`. These examples demonstrate various graph patterns including:

- Simple linear graphs
- Graphs with cycles
- Multi-plan graphs
- Parallel task graphs

Each test graph includes complete DOT specifications and corresponding Python plan/task implementations.

## Best Practices

### Graph Design

1. **Clear Naming**: Use descriptive names for plans and tasks
   ```dot
   plan_data_collection [label="Data Collection Plan"];
   task_fetch_api_data [label="Fetch API Data"];
   ```

2. **Logical Flow**: Design graphs with clear data flow patterns
   ```dot
   plan_data_collection -> task_fetch_data;
   task_fetch_data -> plan_analysis;
   plan_analysis -> task_generate_report;
   ```

3. **Modular Structure**: Break complex graphs into logical components
   ```dot
   // Data collection phase
   plan_data_collection -> task_fetch_data;
   plan_data_collection -> task_validate_data;
   
   // Analysis phase
   task_fetch_data -> plan_analysis;
   task_validate_data -> plan_analysis;
   ```

## Example

### Complete Working Example

**Directory Structure**:
```
data_processing_graph/
├── agent_graph.dot
├── plans/
│   ├── plan_data_collection/
│   │   ├── plan.py
│   │   └── requirements.txt
│   └── plan_analysis/
│       ├── plan.py
│       └── requirements.txt
└── tasks/
    ├── task_fetch_data/
    │   ├── task.py
    │   └── requirements.txt
    └── task_generate_report/
        ├── task.py
        └── requirements.txt
```

**agent_graph.dot**:
```dot
digraph DataProcessingGraph {
    rankdir=TB;
    node [shape=box, style=filled];
    
    plan_data_collection [label="Data Collection Plan", type="plan"];
    plan_analysis [label="Analysis Plan", type="plan"];
    
    task_fetch_data [label="Fetch Data", type="task"];
    task_generate_report [label="Generate Report", type="task"];
    
    plan_data_collection -> task_fetch_data;
    task_fetch_data -> plan_analysis;
    plan_analysis -> task_generate_report;
}
```

**plans/plan_data_collection/plan.py**:
```python
from typing import List

class TaskResult:
    def __init__(self, data):
        self.data = data

class PlanResult:
    def __init__(self, data):
        self.data = data

def plan(upstream_results: List[TaskResult]) -> PlanResult:
    """
    Collect and aggregate data from upstream tasks.
    """
    aggregated_data = []
    for result in upstream_results:
        aggregated_data.extend(result.data)
    
    return PlanResult(data=aggregated_data)
```

**plans/plan_data_collection/requirements.txt**:
```
requests>=2.25.0
pandas>=1.3.0
```

**tasks/task_fetch_data/task.py**:
```python
import requests

class PlanResult:
    def __init__(self, data):
        self.data = data

class TaskResult:
    def __init__(self, data):
        self.data = data

def execute(upstream_plan: PlanResult) -> TaskResult:
    """
    Fetch data from external API based on plan parameters.
    """
    # Extract parameters from plan
    params = upstream_plan.data
    
    # Fetch data
    response = requests.get("https://api.example.com/data", params=params)
    data = response.json()
    
    return TaskResult(data=data)
```

**tasks/task_fetch_data/requirements.txt**:
```
requests>=2.25.0
```

**Java Usage**:
```java
GraphBuilderService service = new GraphBuilderService();
Path specDir = Paths.get("data_processing_graph");
AgentGraph graph = service.buildGraph(specDir);

System.out.println("Graph built successfully!");
System.out.println("Plans: " + graph.planCount());
System.out.println("Tasks: " + graph.taskCount());
```

This example demonstrates a complete working agent graph specification with proper validation, error handling, and clear data flow patterns. 
