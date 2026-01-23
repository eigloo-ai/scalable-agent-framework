import axios, { AxiosError } from 'axios';
import type { AxiosResponse } from 'axios';


import type {
  AgentGraphDto,
  AgentGraphSummary,
  CreateGraphRequest,
  ValidationResult,
  NodeNameValidationRequest,
  ExecutionResponse,
  GraphStatusUpdate,
} from '../types';
import {
  NetworkError,
  ValidationError,
  TenantAccessError,
  FileProcessingError,
} from '../types/errors';
import type { ErrorResponse } from '../types/errors';

// Retry configuration
const MAX_RETRIES = 3;
const INITIAL_RETRY_DELAY = 2000; // 2 seconds - increased from 1 second
const MAX_RETRY_DELAY = 30000; // 30 seconds
const RETRY_STATUS_CODES = [408, 429, 500, 502, 503, 504];

const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
});

// Utility function to delay execution
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

// Utility function to determine if error is retryable
const isRetryableError = (error: AxiosError): boolean => {
  if (!error.response) {
    // Network errors are retryable
    return true;
  }
  
  return RETRY_STATUS_CODES.includes(error.response.status);
};

// Enhanced request wrapper with retry logic
const withRetry = async <T>(
  requestFn: () => Promise<AxiosResponse<T>>,
  retries = MAX_RETRIES
): Promise<T> => {
  try {
    const response = await requestFn();
    return response.data;
  } catch (error) {
    const axiosError = error as AxiosError<ErrorResponse>;
    
    if (retries > 0 && isRetryableError(axiosError)) {
      // Exponential backoff with jitter
      const attemptNumber = MAX_RETRIES - retries + 1;
      const baseDelay = INITIAL_RETRY_DELAY * Math.pow(2, attemptNumber - 1);
      const jitter = Math.random() * 1000; // Add up to 1 second of jitter
      const delayMs = Math.min(baseDelay + jitter, MAX_RETRY_DELAY);
      
      console.log(`API retry attempt ${attemptNumber}/${MAX_RETRIES} after ${delayMs}ms`);
      await delay(delayMs);
      return withRetry(requestFn, retries - 1);
    }
    
    throw transformError(axiosError);
  }
};

// Transform axios errors to application-specific errors
const transformError = (error: AxiosError<ErrorResponse>): Error => {
  if (!error.response) {
    // Network error
    return new NetworkError(
      'Network error occurred. Please check your connection and try again.',
      error
    );
  }

  const { status, data } = error.response;
  const message = data?.message || error.message || 'An unexpected error occurred';

  switch (status) {
    case 400:
      if (data?.error?.includes('validation')) {
        return new ValidationError(message, data?.details?.errors || []);
      }
      return new Error(message);
    
    case 401:
    case 403:
      return new TenantAccessError(
        'Access denied. Please check your tenant permissions.'
      );
    
    case 404:
      return new Error('The requested resource was not found.');
    
    case 409:
      return new Error('Conflict: ' + message);
    
    case 422:
      if (data?.error?.includes('file')) {
        return new FileProcessingError(message);
      }
      return new ValidationError(message, data?.details?.errors || []);
    
    case 429:
      return new Error('Too many requests. Please wait a moment and try again.');
    
    case 500:
    case 502:
    case 503:
    case 504:
      return new Error('Server error occurred. Please try again later.');
    
    default:
      return new Error(message);
  }
};

// Request interceptor for adding common headers
apiClient.interceptors.request.use(
  (config) => {
    // Add timestamp for debugging
    (config as any).metadata = { startTime: Date.now() };
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for logging and error handling
apiClient.interceptors.response.use(
  (response) => {
    // Log successful requests in development
    if (import.meta.env.DEV) {
      const duration = Date.now() - ((response.config as any).metadata?.startTime || 0);
      console.log(`✅ ${response.config.method?.toUpperCase()} ${response.config.url} - ${duration}ms`);
    }
    return response;
  },
  (error: AxiosError<ErrorResponse>) => {
    // Log errors in development
    if (import.meta.env.DEV) {
      const duration = Date.now() - ((error.config as any)?.metadata?.startTime || 0);
      console.error(`❌ ${error.config?.method?.toUpperCase()} ${error.config?.url} - ${duration}ms`, error);
    }
    
    // Don't transform here, let individual API calls handle it
    return Promise.reject(error);
  }
);

// Graph Management API
export const graphApi = {
  listGraphs: (tenantId: string): Promise<AgentGraphSummary[]> =>
    withRetry(() => apiClient.get(`/graphs?tenantId=${tenantId}`)),

  getGraph: (graphId: string, tenantId: string): Promise<AgentGraphDto> =>
    withRetry(() => apiClient.get(`/graphs/${graphId}?tenantId=${tenantId}`)),

  createGraph: (graph: Omit<AgentGraphDto, 'id' | 'createdAt' | 'updatedAt'>): Promise<AgentGraphDto> =>
    withRetry(() => apiClient.post('/graphs', graph)),

  updateGraph: (
    graphId: string,
    graph: AgentGraphDto
  ): Promise<AgentGraphDto> =>
    withRetry(() => apiClient.put(`/graphs/${graphId}`, graph)),

  deleteGraph: (graphId: string, tenantId: string): Promise<void> =>
    withRetry(() => apiClient.delete(`/graphs/${graphId}?tenantId=${tenantId}`)),

  deleteNode: (
    graphId: string,
    nodeId: string,
    tenantId: string
  ): Promise<void> =>
    withRetry(() => apiClient.delete(
      `/graphs/${graphId}/nodes/${nodeId}?tenantId=${tenantId}`
    )),

  deleteEdge: (
    graphId: string,
    edgeId: string,
    tenantId: string
  ): Promise<void> =>
    withRetry(() => apiClient.delete(
      `/graphs/${graphId}/edges/${edgeId}?tenantId=${tenantId}`
    )),

  submitForExecution: (
    graphId: string,
    tenantId: string
  ): Promise<ExecutionResponse> =>
    withRetry(() => apiClient.post(`/graphs/${graphId}/execute?tenantId=${tenantId}`)),

  updateGraphStatus: (
    graphId: string,
    statusUpdate: GraphStatusUpdate
  ): Promise<void> =>
    withRetry(() => apiClient.put(`/graphs/${graphId}/status`, statusUpdate)),
};



// Validation API
export const validationApi = {
  validateGraph: (graph: AgentGraphDto): Promise<ValidationResult> =>
    withRetry(() => apiClient.post('/validation/graph', graph)),

  validateNodeName: (
    request: NodeNameValidationRequest
  ): Promise<ValidationResult> =>
    withRetry(() => apiClient.post('/validation/node-name', request)),
};

export default apiClient;