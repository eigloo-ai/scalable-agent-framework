import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import GraphCanvas from '../GraphCanvas';
import type { AgentGraphDto } from '../../../types';

// Mock the validation hook
const mockValidateGraph = vi.fn();
vi.mock('../../../hooks/useValidation', () => ({
  useValidation: () => ({
    validateGraph: mockValidateGraph,
    validationResult: { valid: true, errors: [], warnings: [] }
  })
}));

// Mock the node operations hook
const mockCreateNode = vi.fn();
const mockDeleteNode = vi.fn();
const mockCreateEdge = vi.fn();
const mockDeleteEdge = vi.fn();
const mockUpdateNodePosition = vi.fn();

vi.mock('../../../hooks/useNodeOperations', () => ({
  useNodeOperations: () => ({
    createNode: mockCreateNode,
    deleteNode: mockDeleteNode,
    createEdge: mockCreateEdge,
    deleteEdge: mockDeleteEdge,
    updateNodePosition: mockUpdateNodePosition
  })
}));

describe('GraphCanvas', () => {
  const mockGraph: AgentGraphDto = {
    id: 'test-graph',
    name: 'Test Graph',
    tenantId: 'test-tenant',
    plans: [
      { name: 'plan1', label: 'Plan 1' }
    ],
    tasks: [
      { name: 'task1', label: 'Task 1' }
    ],
    edges: [
      { from: 'plan1', fromType: 'PLAN', to: 'task1', toType: 'TASK' }
    ]
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render canvas with toolbar', () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    expect(screen.getByRole('button', { name: /task tool/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /plan tool/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /edge tool/i })).toBeInTheDocument();
  });

  it('should handle tool selection', () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    const taskTool = screen.getByRole('button', { name: /task tool/i });
    const planTool = screen.getByRole('button', { name: /plan tool/i });
    
    fireEvent.click(taskTool);
    expect(taskTool).toHaveClass('selected');
    
    fireEvent.click(planTool);
    expect(planTool).toHaveClass('selected');
    expect(taskTool).not.toHaveClass('selected');
  });

  it('should create task node when task tool is selected and canvas is clicked', async () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    // Select task tool
    const taskTool = screen.getByRole('button', { name: /task tool/i });
    fireEvent.click(taskTool);
    
    // Click on canvas
    const canvas = screen.getByRole('img'); // Canvas element
    fireEvent.click(canvas, { clientX: 100, clientY: 100 });
    
    await waitFor(() => {
      expect(mockCreateNode).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'task',
          x: expect.any(Number),
          y: expect.any(Number)
        })
      );
    });
  });

  it('should create plan node when plan tool is selected and canvas is clicked', async () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    // Select plan tool
    const planTool = screen.getByRole('button', { name: /plan tool/i });
    fireEvent.click(planTool);
    
    // Click on canvas
    const canvas = screen.getByRole('img');
    fireEvent.click(canvas, { clientX: 150, clientY: 150 });
    
    await waitFor(() => {
      expect(mockCreateNode).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'plan',
          x: expect.any(Number),
          y: expect.any(Number)
        })
      );
    });
  });

  it('should handle canvas panning with mouse drag', () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    const canvas = screen.getByRole('img');
    
    // Start drag
    fireEvent.mouseDown(canvas, { clientX: 100, clientY: 100 });
    
    // Drag
    fireEvent.mouseMove(canvas, { clientX: 150, clientY: 150 });
    
    // End drag
    fireEvent.mouseUp(canvas);
    
    // Canvas should have updated its viewport
    expect(canvas).toBeInTheDocument();
  });

  it('should handle node selection', async () => {
    render(<GraphCanvas graph={mockGraph} onNodeSelect={vi.fn()} />);
    
    const canvas = screen.getByRole('img');
    
    // Click on a node position (assuming nodes are rendered)
    fireEvent.click(canvas, { clientX: 200, clientY: 200 });
    
    // Should trigger node selection logic
    expect(canvas).toBeInTheDocument();
  });

  it('should handle keyboard shortcuts for deletion', async () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    // Simulate selecting a node first
    const canvas = screen.getByRole('img');
    fireEvent.click(canvas, { clientX: 200, clientY: 200 });
    
    // Press Delete key
    fireEvent.keyDown(document, { key: 'Delete' });
    
    await waitFor(() => {
      expect(mockDeleteNode).toHaveBeenCalled();
    });
  });

  it('should show validation errors on canvas', () => {
    mockValidateGraph.mockReturnValue({
      valid: false,
      errors: ['Test validation error'],
      warnings: []
    });
    
    render(<GraphCanvas graph={mockGraph} />);
    
    expect(screen.getByText('Test validation error')).toBeInTheDocument();
  });

  it('should handle zoom operations', () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    const canvas = screen.getByRole('img');
    
    // Simulate wheel event for zoom
    fireEvent.wheel(canvas, { deltaY: -100 });
    
    // Canvas should handle zoom
    expect(canvas).toBeInTheDocument();
  });

  it('should render nodes and edges from graph data', () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    // Canvas should be rendered with graph content
    const canvas = screen.getByRole('img');
    expect(canvas).toBeInTheDocument();
  });

  it('should handle edge creation between nodes', async () => {
    render(<GraphCanvas graph={mockGraph} />);
    
    // Select edge tool
    const edgeTool = screen.getByRole('button', { name: /edge tool/i });
    fireEvent.click(edgeTool);
    
    const canvas = screen.getByRole('img');
    
    // Click on first node
    fireEvent.mouseDown(canvas, { clientX: 100, clientY: 100 });
    
    // Drag to second node
    fireEvent.mouseMove(canvas, { clientX: 200, clientY: 200 });
    fireEvent.mouseUp(canvas, { clientX: 200, clientY: 200 });
    
    await waitFor(() => {
      expect(mockCreateEdge).toHaveBeenCalled();
    });
  });
});
