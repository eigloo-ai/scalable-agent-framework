export interface ExecutorFile {
  name: string;
  contents: string;
  creationDate: string;
  version: string;
  updateDate: string;
}

export interface PlanDto {
  name: string;
  label: string;
  upstreamTaskIds: string[];
  files: ExecutorFile[];
}

export interface TaskDto {
  name: string;
  label: string;
  upstreamPlanId: string;
  files: ExecutorFile[];
}

export type GraphStatus = 'NEW' | 'ACTIVE' | 'ARCHIVED';
export type GraphRunStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED' | 'UNKNOWN';

export interface AgentGraphDto {
  id: string;
  name: string;
  tenantId: string;
  status: GraphStatus;
  plans: PlanDto[];
  tasks: TaskDto[];
  planToTasks: Record<string, string[]>;
  taskToPlan: Record<string, string>;
  createdAt: string;
  updatedAt: string;
}

export interface AgentGraphSummary {
  id: string;
  name: string;
  status: GraphStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGraphRequest {
  name: string;
  tenantId: string;
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export interface NodeNameValidationRequest {
  nodeName: string;
  graphId: string;
  excludeNodeId?: string;
  existingNodeNames?: string[];
}

export interface ExecutionResponse {
  executionId: string;
  status: string;
  message: string;
}

export interface GraphStatusUpdate {
  status: GraphStatus;
}

export interface GraphRunSummary {
  tenantId: string;
  graphId: string;
  lifetimeId: string;
  status: GraphRunStatus | string;
  entryPlanNames: string[];
  errorMessage: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
}

export interface RunTimelineEvent {
  eventType: 'PLAN_EXECUTION' | 'TASK_EXECUTION' | string;
  nodeName: string;
  executionId: string;
  status: string;
  createdAt: string | null;
  persistedAt: string | null;
  parentNodeName: string | null;
  parentExecutionId: string | null;
  nextTaskNames?: string[] | null;
  errorMessage?: string | null;
}

export interface RunTimelineResponse {
  tenantId: string;
  graphId: string;
  lifetimeId: string;
  status: GraphRunStatus | string;
  entryPlanNames: string[];
  errorMessage: string | null;
  createdAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  planExecutions: number;
  taskExecutions: number;
  events: RunTimelineEvent[];
}

// Re-export error types for convenience
export type {
  ApiError,
  ErrorResponse,
  NetworkError,
  ValidationError,
  TenantAccessError,
  FileProcessingError,
} from './errors';
