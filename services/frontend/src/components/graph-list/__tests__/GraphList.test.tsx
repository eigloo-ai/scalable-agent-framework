import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import GraphList from '../GraphList';
import type { AgentGraphSummary } from '../../../types';

describe('GraphList', () => {
  const mockGraphs: AgentGraphSummary[] = [
    {
      id: 'graph1',
      name: 'Test Graph 1',
      status: 'NEW',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    {
      id: 'graph2',
      name: 'Test Graph 2',
      status: 'ACTIVE',
      createdAt: '2024-01-02T00:00:00Z',
      updatedAt: '2024-01-02T00:00:00Z'
    },
    {
      id: 'graph3',
      name: 'Test Graph 3',
      status: 'ARCHIVED',
      createdAt: '2024-01-03T00:00:00Z',
      updatedAt: '2024-01-03T00:00:00Z'
    }
  ];

  const mockOnLoadGraph = vi.fn();
  const mockOnDeleteGraph = vi.fn();
  const mockOnSubmitForExecution = vi.fn();
  const mockOnCreateNew = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render list of graphs', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    expect(screen.getByText('Test Graph 1')).toBeInTheDocument();
    expect(screen.getByText('Test Graph 2')).toBeInTheDocument();
    expect(screen.getByText('Test Graph 3')).toBeInTheDocument();
  });

  it('should display graph status correctly', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    expect(screen.getByText('NEW')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.getByText('ARCHIVED')).toBeInTheDocument();
  });

  it('should display creation and modification dates', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    // Should show formatted dates (using getAllByText for multiple matches)
    expect(screen.getAllByText(/1\/1\/2024/)).toHaveLength(2); // Created and updated for graph2
    expect(screen.getAllByText(/1\/2\/2024/)).toHaveLength(2); // Created and updated for graph3
    expect(screen.getAllByText(/12\/31\/2023/)).toHaveLength(2); // Created and updated for graph1
  });

  it('should call onLoadGraph when Load button is clicked', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    const loadButtons = screen.getAllByText('Load');
    fireEvent.click(loadButtons[0]);

    expect(mockOnLoadGraph).toHaveBeenCalledWith('graph3'); // First in sorted order (most recent)
  });

  it('should show delete confirmation dialog', async () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    const deleteButtons = screen.getAllByText('Delete');
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('Confirm Delete')).toBeInTheDocument();
      expect(screen.getByText(/Are you sure you want to delete this graph/)).toBeInTheDocument();
    });
  });

  it('should delete graph when confirmed', async () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    // Click delete button
    const deleteButtons = screen.getAllByText('Delete');
    fireEvent.click(deleteButtons[0]);

    // Confirm deletion - use className to target the specific confirm button
    await waitFor(() => {
      const confirmButton = document.querySelector('.confirm-delete-button');
      expect(confirmButton).toBeInTheDocument();
      fireEvent.click(confirmButton!);
    });

    expect(mockOnDeleteGraph).toHaveBeenCalledWith('graph3'); // First in sorted order
  });

  it('should cancel delete when Cancel is clicked', async () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    // Click delete button
    const deleteButtons = screen.getAllByText('Delete');
    fireEvent.click(deleteButtons[0]);

    // Cancel deletion
    await waitFor(() => {
      const cancelButton = screen.getByText('Cancel');
      fireEvent.click(cancelButton);
    });

    await waitFor(() => {
      expect(screen.queryByText('Confirm Delete')).not.toBeInTheDocument();
    });

    expect(mockOnDeleteGraph).not.toHaveBeenCalled();
  });

  it('should handle submit for execution', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    const executeButtons = screen.getAllByText('Execute');
    fireEvent.click(executeButtons[1]);

    expect(mockOnSubmitForExecution).toHaveBeenCalledWith('graph2');
  });

  it('should disable execute button for archived graphs', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    const executeButtons = screen.getAllByText('Execute');
    
    // Find the archived graph's execute button (graph3 with ARCHIVED status)
    // Since graphs are sorted by updatedAt desc, graph3 is first, graph2 is second
    expect(executeButtons[0]).toBeDisabled(); // graph3 (ARCHIVED)
    expect(executeButtons[1]).not.toBeDisabled(); // graph2 (ACTIVE)
    expect(executeButtons[2]).not.toBeDisabled(); // graph1 (NEW)
  });

  it('should show create new button when no graphs exist', () => {
    render(
      <GraphList
        graphs={[]}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    expect(screen.getByText('No graphs found')).toBeInTheDocument();
    expect(screen.getByText('Create New Graph')).toBeInTheDocument();
  });

  it('should call onCreateNew when Create New Graph is clicked', () => {
    render(
      <GraphList
        graphs={[]}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    const createButton = screen.getByText('Create New Graph');
    fireEvent.click(createButton);

    expect(mockOnCreateNew).toHaveBeenCalled();
  });

  it('should handle loading state', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={true}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    expect(screen.getByText('Loading graphs...')).toBeInTheDocument();
  });

  it('should handle error state', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error="Failed to load graphs"
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    expect(screen.getByText('Error: Failed to load graphs')).toBeInTheDocument();
    expect(screen.getByText('Create New Graph')).toBeInTheDocument(); // Error state shows create button, not retry
  });

  it('should sort graphs by update date by default', () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    const graphNames = screen.getAllByText(/Test Graph \d/);
    expect(graphNames[0]).toHaveTextContent('Test Graph 3'); // Most recent first
    expect(graphNames[1]).toHaveTextContent('Test Graph 2');
    expect(graphNames[2]).toHaveTextContent('Test Graph 1');
  });

  it('should filter graphs by status', async () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    // Click status filter
    const statusFilter = screen.getByLabelText('Filter by status:');
    fireEvent.change(statusFilter, { target: { value: 'ACTIVE' } });

    await waitFor(() => {
      expect(screen.getByText('Test Graph 2')).toBeInTheDocument();
      expect(screen.queryByText('Test Graph 1')).not.toBeInTheDocument();
      expect(screen.queryByText('Test Graph 3')).not.toBeInTheDocument();
    });
  });

  it('should search graphs by name', async () => {
    render(
      <GraphList
        graphs={mockGraphs}
        loading={false}
        error={null}
        onLoadGraph={mockOnLoadGraph}
        onDeleteGraph={mockOnDeleteGraph}
        onSubmitForExecution={mockOnSubmitForExecution}
        onCreateNew={mockOnCreateNew}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search graphs...');
    fireEvent.change(searchInput, { target: { value: 'Graph 2' } });

    await waitFor(() => {
      expect(screen.getByText('Test Graph 2')).toBeInTheDocument();
      expect(screen.queryByText('Test Graph 1')).not.toBeInTheDocument();
      expect(screen.queryByText('Test Graph 3')).not.toBeInTheDocument();
    });
  });
});
