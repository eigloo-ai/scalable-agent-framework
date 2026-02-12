import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import GraphCanvas from '../GraphCanvas';
import type { AgentGraphDto } from '../../../types';

// Mock the hooks with more realistic implementations
const mockValidateGraph = vi.fn();
const mockCreateNode = vi.fn();
const mockDeleteNode = vi.fn();
const mockCreateEdge = vi.fn();
const mockDeleteEdge = vi.fn();
const mockUpdateNodePosition = vi.fn();

vi.mock('../../../hooks/useValidation', () => ({
  useValidation: () => ({
    validateGraph: mockValidateGraph,
    validationResult: { valid: true, errors: [], warnings: [] }
  })
}));

vi.mock('../../../hooks/useNodeOperations', () => ({
  useNodeOperations: () => ({
    createNode: mockCreateNode,
    deleteNode: mockDeleteNode,
    createEdge: mockCreateEdge,
    deleteEdge: mockDeleteEdge,
    updateNodePosition: mockUpdateNodePosition
  })
}));

// Mock canvas context for drawing operations
const mockCanvasContext = {
  clearRect: vi.fn(),
  fillRect: vi.fn(),
  strokeRect: vi.fn(),
  arc: vi.fn(),
  fill: vi.fn(),
  stroke: vi.fn(),
  beginPath: vi.fn(),
  closePath: vi.fn(),
  moveTo: vi.fn(),
  lineTo: vi.fn(),
  save: vi.fn(),
  restore: vi.fn(),
  translate: vi.fn(),
  scale: vi.fn(),
  setTransform: vi.fn(),
  getImageData: vi.fn(),
  putImageData: vi.fn(),
  measureText: vi.fn(() => ({ width: 100 })),
  fillText: vi.fn(),
  strokeText: vi.fn()
};

// Mock HTMLCanvasElement.getContext
HTMLCanvasElement.prototype.getContext = vi.fn(() => mockCanvasContext);

describe('GraphCanvas Integration Tests', () => {
  const mockGraph: AgentGraphDto = {
    id: 'test-graph',
    name: 'Test Graph',
    tenantId: 'test-tenant',
    plans: [
      { 
        name: 'plan1', 
        label: 'Plan 1', 
        position: { x: 200, y: 100 }
      }
    ],
    tasks: [
      { 
        name: 'task1', 
        label: 'Task 1',
        position: { x: 100, y: 100 }
      }
    ],
    edges: [
      { from: 'plan1', fromType: 'PLAN', to: 'task1', toType: 'TASK' }
    ]
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should handle complete node creation workflow', async () => {
    const mockOnNodeSelect = vi.fn();
    
    render(
      <GraphCanvas 
        graph={mockGraph} 
        onNodeSelect={mockOnNodeSelect}
      />
    );

    // Select task tool
    const taskTool = screen.getByRole('button', { name: /task tool/i });
    fireEvent.click(taskTool);

    // Click on canvas to create node
    const canvas = screen.getByRole('img');
    fireEvent.click(canvas, { clientX: 300, clientY: 200 });

    // Should trigger node creation
    await waitFor(() => {
      expect(mockCreateNode).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'task',
          x: expect.any(Number),
          y: expect.any(Number)
        })
      );
    });

    // Should validate graph after creation
    expect(mockValidateGraph).toHaveBeenCalled();
  });

  it('should handle edge creation between existing nodes', async () => {
    render(<GraphCanvas graph={mockGraph} />);

    // Select edge tool
    const edgeTool = screen.getByRole('button', { name: /edge tool/i });
    fireEvent.click(edgeTool);

    const canvas = screen.getByRole('img');

    // Start edge creation from task1 position
    fireEvent.mouseDown(canvas, { clientX: 100, clientY: 100 });
    
    // Drag to plan1 position
    fireEvent.mouseMove(canvas, { clientX: 200, clientY: 100 });
    
    // End edge creation
    fireEvent.mouseUp(canvas, { clientX: 200, clientY: 100 });

    await waitFor(() => {
      expect(mockCreateEdge).toHaveBeenCalledWith(
        expect.objectContaining({
          fromNodeId: 'task1',
          toNodeId: 'plan1'
        })
      );
    });
  });

  it('should handle node dragging and position updates', async () => {
    render(<GraphCanvas graph={mockGraph} />);

    const canvas = screen.getByRole('img');

    // Click on task1 to select it
    fireEvent.mouseDown(canvas, { clientX: 100, clientY: 100 });
    
    // Drag to new position
    fireEvent.mouseMove(canvas, { clientX: 150, clientY: 150 });
    
    // Release
    fireEvent.mouseUp(canvas, { clientX: 150, clientY: 150 });

    await waitFor(() => {
      expect(mockUpdateNodePosition).toHaveBeenCalledWith(
        'task1',
        expect.objectContaining({
          x: expect.any(Number),
          y: expect.any(Number)
        })
      );
    });
  });

  it('should handle canvas panning and zoom', () => {
    render(<GraphCanvas graph={mockGraph} />);

    const canvas = screen.getByRole('img');

    // Test panning
    fireEvent.mouseDown(canvas, { clientX: 400, clientY: 300 });
    fireEvent.mouseMove(canvas, { clientX: 450, clientY: 350 });
    fireEvent.mouseUp(canvas, { clientX: 450, clientY: 350 });

    // Test zoom
    fireEvent.wheel(canvas, { deltaY: -100 });
    fireEvent.wheel(canvas, { deltaY: 100 });

    // Canvas should handle these operations
    expect(mockCanvasContext.setTransform).toHaveBeenCalled();
  });

  it('should handle multi-selection with Ctrl+click', async () => {
    const mockOnNodeSelect = vi.fn();
    
    render(
      <GraphCanvas 
        graph={mockGraph} 
        onNodeSelect={mockOnNodeSelect}
      />
    );

    const canvas = screen.getByRole('img');

    // Select first node
    fireEvent.click(canvas, { clientX: 100, clientY: 100 });
    
    // Add second node to selection with Ctrl
    fireEvent.click(canvas, { 
      clientX: 200, 
      clientY: 100, 
      ctrlKey: true 
    });

    await waitFor(() => {
      expect(mockOnNodeSelect).toHaveBeenCalledTimes(2);
    });
  });

  it('should handle keyboard shortcuts for operations', async () => {
    render(<GraphCanvas graph={mockGraph} />);

    const canvas = screen.getByRole('img');

    // Select a node first
    fireEvent.click(canvas, { clientX: 100, clientY: 100 });

    // Press Delete key
    fireEvent.keyDown(document, { key: 'Delete' });

    await waitFor(() => {
      expect(mockDeleteNode).toHaveBeenCalledWith('task1');
    });

    // Press Escape to deselect
    fireEvent.keyDown(document, { key: 'Escape' });

    // Press Ctrl+A to select all
    fireEvent.keyDown(document, { key: 'a', ctrlKey: true });
  });

  it('should handle validation errors during operations', async () => {
    mockValidateGraph.mockReturnValue({
      valid: false,
      errors: ['Tasks must have exactly one upstream plan'],
      warnings: []
    });

    render(<GraphCanvas graph={mockGraph} />);

    // Try to create an invalid connection
    const edgeTool = screen.getByRole('button', { name: /edge tool/i });
    fireEvent.click(edgeTool);

    const canvas = screen.getByRole('img');
    fireEvent.mouseDown(canvas, { clientX: 100, clientY: 100 });
    fireEvent.mouseMove(canvas, { clientX: 200, clientY: 100 });
    fireEvent.mouseUp(canvas, { clientX: 200, clientY: 100 });

    await waitFor(() => {
      expect(screen.getByText('Tasks must have exactly one upstream plan')).toBeInTheDocument();
    });
  });

  it('should handle undo/redo operations', async () => {
    render(<GraphCanvas graph={mockGraph} />);

    // Create a node
    const taskTool = screen.getByRole('button', { name: /task tool/i });
    fireEvent.click(taskTool);

    const canvas = screen.getByRole('img');
    fireEvent.click(canvas, { clientX: 300, clientY: 200 });

    await waitFor(() => {
      expect(mockCreateNode).toHaveBeenCalled();
    });

    // Undo with Ctrl+Z
    fireEvent.keyDown(document, { key: 'z', ctrlKey: true });

    // Redo with Ctrl+Y
    fireEvent.keyDown(document, { key: 'y', ctrlKey: true });
  });

  it('should handle canvas resize and viewport updates', () => {
    const { rerender } = render(<GraphCanvas graph={mockGraph} />);

    // Simulate window resize
    fireEvent(window, new Event('resize'));

    // Re-render with different dimensions
    rerender(<GraphCanvas graph={mockGraph} />);

    // Canvas should adapt to new size
    expect(mockCanvasContext.setTransform).toHaveBeenCalled();
  });

  it('should handle touch events for mobile interaction', () => {
    render(<GraphCanvas graph={mockGraph} />);

    const canvas = screen.getByRole('img');

    // Touch start
    fireEvent.touchStart(canvas, {
      touches: [{ clientX: 100, clientY: 100 }]
    });

    // Touch move
    fireEvent.touchMove(canvas, {
      touches: [{ clientX: 150, clientY: 150 }]
    });

    // Touch end
    fireEvent.touchEnd(canvas, {
      changedTouches: [{ clientX: 150, clientY: 150 }]
    });

    // Should handle touch interactions similar to mouse
    expect(mockCanvasContext.setTransform).toHaveBeenCalled();
  });

  it('should handle performance optimization with large graphs', () => {
    const largeGraph: AgentGraphDto = {
      ...mockGraph,
      plans: Array.from({ length: 100 }, (_, i) => ({
        name: `plan${i}`,
        label: `Plan ${i}`,
        position: { x: (i % 10) * 100, y: Math.floor(i / 10) * 100 }
      })),
      tasks: Array.from({ length: 100 }, (_, i) => ({
        name: `task${i}`,
        label: `Task ${i}`,
        position: { x: (i % 10) * 100 + 50, y: Math.floor(i / 10) * 100 + 50 }
      }))
    };

    render(<GraphCanvas graph={largeGraph} />);

    // Should render without performance issues
    expect(mockCanvasContext.clearRect).toHaveBeenCalled();
  });

  it('should handle edge snapping to node boundaries', async () => {
    render(<GraphCanvas graph={mockGraph} />);

    const edgeTool = screen.getByRole('button', { name: /edge tool/i });
    fireEvent.click(edgeTool);

    const canvas = screen.getByRole('img');

    // Start edge near but not exactly on node center
    fireEvent.mouseDown(canvas, { clientX: 95, clientY: 105 });
    
    // End edge near another node
    fireEvent.mouseMove(canvas, { clientX: 205, clientY: 95 });
    fireEvent.mouseUp(canvas, { clientX: 205, clientY: 95 });

    await waitFor(() => {
      expect(mockCreateEdge).toHaveBeenCalledWith(
        expect.objectContaining({
          fromNodeId: 'task1',
          toNodeId: 'plan1'
        })
      );
    });
  });
});
