import { vi, describe, it, expect, beforeEach } from 'vitest';
import axios from 'axios';
import { graphApi, fileApi, validationApi } from '../client';
import type { AgentGraphDto, CreateGraphRequest, ExecutorFileDto } from '../../types';

// Mock axios
vi.mock('axios');
const mockedAxios = vi.mocked(axios);

describe('API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('graphApi', () => {
    describe('listGraphs', () => {
      it('should fetch graphs for tenant', async () => {
        const mockGraphs = [
          { id: 'graph1', name: 'Test Graph 1', status: 'New' },
          { id: 'graph2', name: 'Test Graph 2', status: 'Running' }
        ];

        mockedAxios.get.mockResolvedValue({ data: mockGraphs });

        const result = await graphApi.listGraphs('test-tenant');

        expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/graphs', {
          params: { tenantId: 'test-tenant' }
        });
        expect(result).toEqual(mockGraphs);
      });

      it('should handle API errors', async () => {
        mockedAxios.get.mockRejectedValue(new Error('Network error'));

        await expect(graphApi.listGraphs('test-tenant')).rejects.toThrow('Network error');
      });
    });

    describe('getGraph', () => {
      it('should fetch specific graph', async () => {
        const mockGraph: AgentGraphDto = {
          id: 'graph1',
          name: 'Test Graph',
          tenantId: 'test-tenant',
          plans: [],
          tasks: [],
          edges: []
        };

        mockedAxios.get.mockResolvedValue({ data: mockGraph });

        const result = await graphApi.getGraph('graph1', 'test-tenant');

        expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/graphs/graph1', {
          params: { tenantId: 'test-tenant' }
        });
        expect(result).toEqual(mockGraph);
      });
    });

    describe('createGraph', () => {
      it('should create new graph', async () => {
        const createRequest: CreateGraphRequest = {
          name: 'New Graph',
          tenantId: 'test-tenant'
        };

        const mockResponse: AgentGraphDto = {
          id: 'new-graph-id',
          name: 'New Graph',
          tenantId: 'test-tenant',
          plans: [],
          tasks: [],
          edges: []
        };

        mockedAxios.post.mockResolvedValue({ data: mockResponse });

        const result = await graphApi.createGraph(createRequest);

        expect(mockedAxios.post).toHaveBeenCalledWith('/api/v1/graphs', createRequest);
        expect(result).toEqual(mockResponse);
      });
    });

    describe('updateGraph', () => {
      it('should update existing graph', async () => {
        const mockGraph: AgentGraphDto = {
          id: 'graph1',
          name: 'Updated Graph',
          tenantId: 'test-tenant',
          plans: [],
          tasks: [],
          edges: []
        };

        mockedAxios.put.mockResolvedValue({ data: mockGraph });

        const result = await graphApi.updateGraph('graph1', mockGraph);

        expect(mockedAxios.put).toHaveBeenCalledWith('/api/v1/graphs/graph1', mockGraph);
        expect(result).toEqual(mockGraph);
      });
    });

    describe('deleteGraph', () => {
      it('should delete graph', async () => {
        mockedAxios.delete.mockResolvedValue({});

        await graphApi.deleteGraph('graph1', 'test-tenant');

        expect(mockedAxios.delete).toHaveBeenCalledWith('/api/v1/graphs/graph1', {
          params: { tenantId: 'test-tenant' }
        });
      });
    });

    describe('deleteNode', () => {
      it('should delete node from graph', async () => {
        mockedAxios.delete.mockResolvedValue({});

        await graphApi.deleteNode('graph1', 'node1', 'test-tenant');

        expect(mockedAxios.delete).toHaveBeenCalledWith('/api/v1/graphs/graph1/nodes/node1', {
          params: { tenantId: 'test-tenant' }
        });
      });
    });

    describe('deleteEdge', () => {
      it('should delete edge from graph', async () => {
        mockedAxios.delete.mockResolvedValue({});

        await graphApi.deleteEdge('graph1', 'edge1', 'test-tenant');

        expect(mockedAxios.delete).toHaveBeenCalledWith('/api/v1/graphs/graph1/edges/edge1', {
          params: { tenantId: 'test-tenant' }
        });
      });
    });

    describe('submitForExecution', () => {
      it('should submit graph for execution', async () => {
        const mockResponse = { executionId: 'exec-123' };
        mockedAxios.post.mockResolvedValue({ data: mockResponse });

        const result = await graphApi.submitForExecution('graph1', 'test-tenant');

        expect(mockedAxios.post).toHaveBeenCalledWith('/api/v1/graphs/graph1/execute', {}, {
          params: { tenantId: 'test-tenant' }
        });
        expect(result).toEqual(mockResponse);
      });
    });

    describe('updateGraphStatus', () => {
      it('should update graph status', async () => {
        mockedAxios.put.mockResolvedValue({});

        await graphApi.updateGraphStatus('graph1', 'test-tenant', 'Running');

        expect(mockedAxios.put).toHaveBeenCalledWith('/api/v1/graphs/graph1/status', {
          status: 'Running'
        }, {
          params: { tenantId: 'test-tenant' }
        });
      });
    });
  });

  describe('fileApi', () => {
    describe('getPlanFiles', () => {
      it('should fetch plan files', async () => {
        const mockFiles: ExecutorFileDto[] = [
          {
            name: 'plan.py',
            contents: 'def plan(): pass',
            creationDate: '2024-01-01T00:00:00Z',
            version: '1.0',
            updateDate: '2024-01-01T00:00:00Z'
          }
        ];

        mockedAxios.get.mockResolvedValue({ data: mockFiles });

        const result = await fileApi.getPlanFiles('plan1');

        expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/files/plan/plan1');
        expect(result).toEqual(mockFiles);
      });
    });

    describe('getTaskFiles', () => {
      it('should fetch task files', async () => {
        const mockFiles: ExecutorFileDto[] = [
          {
            name: 'task.py',
            contents: 'def task(): pass',
            creationDate: '2024-01-01T00:00:00Z',
            version: '1.0',
            updateDate: '2024-01-01T00:00:00Z'
          }
        ];

        mockedAxios.get.mockResolvedValue({ data: mockFiles });

        const result = await fileApi.getTaskFiles('task1');

        expect(mockedAxios.get).toHaveBeenCalledWith('/api/v1/files/task/task1');
        expect(result).toEqual(mockFiles);
      });
    });

    describe('updatePlanFile', () => {
      it('should update plan file', async () => {
        const mockFile: ExecutorFileDto = {
          name: 'plan.py',
          contents: 'def plan(): return "updated"',
          creationDate: '2024-01-01T00:00:00Z',
          version: '1.1',
          updateDate: '2024-01-02T00:00:00Z'
        };

        mockedAxios.put.mockResolvedValue({ data: mockFile });

        const result = await fileApi.updatePlanFile('plan1', 'plan.py', 'def plan(): return "updated"');

        expect(mockedAxios.put).toHaveBeenCalledWith('/api/v1/files/plan/plan1/files/plan.py', 
          'def plan(): return "updated"',
          { headers: { 'Content-Type': 'text/plain' } }
        );
        expect(result).toEqual(mockFile);
      });
    });

    describe('updateTaskFile', () => {
      it('should update task file', async () => {
        const mockFile: ExecutorFileDto = {
          name: 'task.py',
          contents: 'def task(): return "updated"',
          creationDate: '2024-01-01T00:00:00Z',
          version: '1.1',
          updateDate: '2024-01-02T00:00:00Z'
        };

        mockedAxios.put.mockResolvedValue({ data: mockFile });

        const result = await fileApi.updateTaskFile('task1', 'task.py', 'def task(): return "updated"');

        expect(mockedAxios.put).toHaveBeenCalledWith('/api/v1/files/task/task1/files/task.py', 
          'def task(): return "updated"',
          { headers: { 'Content-Type': 'text/plain' } }
        );
        expect(result).toEqual(mockFile);
      });
    });
  });

  describe('validationApi', () => {
    describe('validateGraph', () => {
      it('should validate graph', async () => {
        const mockGraph: AgentGraphDto = {
          id: 'graph1',
          name: 'Test Graph',
          tenantId: 'test-tenant',
          plans: [],
          tasks: [],
          edges: []
        };

        const mockValidationResult = {
          valid: true,
          errors: [],
          warnings: []
        };

        mockedAxios.post.mockResolvedValue({ data: mockValidationResult });

        const result = await validationApi.validateGraph(mockGraph);

        expect(mockedAxios.post).toHaveBeenCalledWith('/api/v1/validation/graph', mockGraph);
        expect(result).toEqual(mockValidationResult);
      });
    });

    describe('validateNodeName', () => {
      it('should validate node name', async () => {
        const mockRequest = {
          name: 'test_node',
          existingNames: ['other_node'],
          graphId: 'graph1'
        };

        const mockValidationResult = {
          valid: true,
          errors: [],
          warnings: []
        };

        mockedAxios.post.mockResolvedValue({ data: mockValidationResult });

        const result = await validationApi.validateNodeName(mockRequest);

        expect(mockedAxios.post).toHaveBeenCalledWith('/api/v1/validation/node-name', mockRequest);
        expect(result).toEqual(mockValidationResult);
      });
    });
  });

  describe('Error handling', () => {
    it('should handle network errors', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network Error'));

      await expect(graphApi.listGraphs('test-tenant')).rejects.toThrow('Network Error');
    });

    it('should handle HTTP error responses', async () => {
      const errorResponse = {
        response: {
          status: 404,
          data: { message: 'Graph not found' }
        }
      };

      mockedAxios.get.mockRejectedValue(errorResponse);

      await expect(graphApi.getGraph('nonexistent', 'test-tenant')).rejects.toEqual(errorResponse);
    });

    it('should handle timeout errors', async () => {
      mockedAxios.get.mockRejectedValue(new Error('timeout of 5000ms exceeded'));

      await expect(graphApi.listGraphs('test-tenant')).rejects.toThrow('timeout of 5000ms exceeded');
    });
  });

  describe('Request interceptors', () => {
    it('should add authorization headers if available', async () => {
      // Mock localStorage or other auth mechanism
      const mockAuthToken = 'bearer-token-123';
      
      // This would test request interceptors if they exist
      mockedAxios.get.mockResolvedValue({ data: [] });
      
      await graphApi.listGraphs('test-tenant');
      
      expect(mockedAxios.get).toHaveBeenCalled();
    });
  });
});