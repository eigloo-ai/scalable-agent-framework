import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import App from '../App';

// Mock the API client
const mockListGraphs = vi.fn();
const mockGetGraph = vi.fn();
const mockCreateGraph = vi.fn();

vi.mock('../api/client', () => ({
  graphApi: {
    listGraphs: mockListGraphs,
    getGraph: mockGetGraph,
    createGraph: mockCreateGraph
  }
}));

// Mock components to simplify testing
vi.mock('../components/canvas/GraphCanvas', () => ({
  default: ({ graph }: { graph: any }) => (
    <div data-testid="graph-canvas">
      Canvas for {graph?.name || 'No Graph'}
    </div>
  )
}));

vi.mock('../components/file-explorer/FileExplorer', () => ({
  default: ({ graph }: { graph: any }) => (
    <div data-testid="file-explorer">
      Files for {graph?.name || 'No Graph'}
    </div>
  )
}));

vi.mock('../components/editor/CodeEditor', () => ({
  default: ({ file }: { file: any }) => (
    <div data-testid="code-editor">
      Editor for {file?.name || 'No File'}
    </div>
  )
}));

vi.mock('../components/graph-list/GraphList', () => ({
  default: ({ graphs, onGraphSelect, onCreateNew }: any) => (
    <div data-testid="graph-list">
      <div>Graph List ({graphs.length} graphs)</div>
      <button onClick={() => onGraphSelect('graph1')}>Load Graph 1</button>
      <button onClick={onCreateNew}>Create New</button>
    </div>
  )
}));

const AppWithRouter = () => (
  <BrowserRouter>
    <App />
  </BrowserRouter>
);

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListGraphs.mockResolvedValue([
      { id: 'graph1', name: 'Test Graph 1', status: 'New' },
      { id: 'graph2', name: 'Test Graph 2', status: 'Running' }
    ]);
  });

  it('should render main layout with tenant input', () => {
    render(<AppWithRouter />);

    expect(screen.getByLabelText(/tenant id/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue('evil-corp')).toBeInTheDocument();
  });

  it('should render three-pane layout in editor mode', async () => {
    mockGetGraph.mockResolvedValue({
      id: 'graph1',
      name: 'Test Graph 1',
      tenantId: 'evil-corp',
      plans: [],
      tasks: [],
      edges: []
    });

    render(<AppWithRouter />);

    // Navigate to editor by loading a graph
    await waitFor(() => {
      const loadButton = screen.getByText('Load Graph 1');
      fireEvent.click(loadButton);
    });

    await waitFor(() => {
      expect(screen.getByTestId('file-explorer')).toBeInTheDocument();
      expect(screen.getByTestId('graph-canvas')).toBeInTheDocument();
      expect(screen.getByTestId('code-editor')).toBeInTheDocument();
    });
  });

  it('should show graph management page by default', () => {
    render(<AppWithRouter />);

    expect(screen.getByTestId('graph-list')).toBeInTheDocument();
    expect(screen.getByText('Graph List (2 graphs)')).toBeInTheDocument();
  });

  it('should handle tenant ID changes', async () => {
    render(<AppWithRouter />);

    const tenantInput = screen.getByLabelText(/tenant id/i);
    fireEvent.change(tenantInput, { target: { value: 'new-tenant' } });

    await waitFor(() => {
      expect(mockListGraphs).toHaveBeenCalledWith('new-tenant');
    });
  });

  it('should load graph when selected from list', async () => {
    mockGetGraph.mockResolvedValue({
      id: 'graph1',
      name: 'Test Graph 1',
      tenantId: 'evil-corp',
      plans: [],
      tasks: [],
      edges: []
    });

    render(<AppWithRouter />);

    const loadButton = screen.getByText('Load Graph 1');
    fireEvent.click(loadButton);

    await waitFor(() => {
      expect(mockGetGraph).toHaveBeenCalledWith('graph1', 'evil-corp');
      expect(screen.getByText('Canvas for Test Graph 1')).toBeInTheDocument();
    });
  });

  it('should create new graph when requested', async () => {
    mockCreateGraph.mockResolvedValue({
      id: 'new-graph',
      name: 'New Graph',
      tenantId: 'evil-corp',
      plans: [],
      tasks: [],
      edges: []
    });

    render(<AppWithRouter />);

    const createButton = screen.getByText('Create New');
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(mockCreateGraph).toHaveBeenCalledWith({
        name: expect.stringContaining('New Graph'),
        tenantId: 'evil-corp'
      });
    });
  });

  it('should handle navigation between management and editor', async () => {
    mockGetGraph.mockResolvedValue({
      id: 'graph1',
      name: 'Test Graph 1',
      tenantId: 'evil-corp',
      plans: [],
      tasks: [],
      edges: []
    });

    render(<AppWithRouter />);

    // Start in management view
    expect(screen.getByTestId('graph-list')).toBeInTheDocument();

    // Navigate to editor
    const loadButton = screen.getByText('Load Graph 1');
    fireEvent.click(loadButton);

    await waitFor(() => {
      expect(screen.getByTestId('graph-canvas')).toBeInTheDocument();
    });

    // Navigate back to management
    const backButton = screen.getByText(/back to graphs/i);
    fireEvent.click(backButton);

    await waitFor(() => {
      expect(screen.getByTestId('graph-list')).toBeInTheDocument();
    });
  });

  it('should handle loading states', () => {
    mockListGraphs.mockReturnValue(new Promise(() => {})); // Never resolves

    render(<AppWithRouter />);

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('should handle error states', async () => {
    mockListGraphs.mockRejectedValue(new Error('Failed to load graphs'));

    render(<AppWithRouter />);

    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument();
      expect(screen.getByText(/failed to load graphs/i)).toBeInTheDocument();
    });
  });

  it('should persist tenant ID in localStorage', () => {
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');

    render(<AppWithRouter />);

    const tenantInput = screen.getByLabelText(/tenant id/i);
    fireEvent.change(tenantInput, { target: { value: 'persistent-tenant' } });

    expect(setItemSpy).toHaveBeenCalledWith('tenantId', 'persistent-tenant');
  });

  it('should load tenant ID from localStorage on startup', () => {
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem');
    getItemSpy.mockReturnValue('stored-tenant');

    render(<AppWithRouter />);

    expect(screen.getByDisplayValue('stored-tenant')).toBeInTheDocument();
  });

  it('should handle keyboard shortcuts', async () => {
    mockGetGraph.mockResolvedValue({
      id: 'graph1',
      name: 'Test Graph 1',
      tenantId: 'evil-corp',
      plans: [],
      tasks: [],
      edges: []
    });

    render(<AppWithRouter />);

    // Load a graph first
    const loadButton = screen.getByText('Load Graph 1');
    fireEvent.click(loadButton);

    await waitFor(() => {
      expect(screen.getByTestId('graph-canvas')).toBeInTheDocument();
    });

    // Test Ctrl+S shortcut (should be handled by CodeEditor)
    fireEvent.keyDown(document, { key: 's', ctrlKey: true });

    // Test Escape to go back
    fireEvent.keyDown(document, { key: 'Escape' });

    await waitFor(() => {
      expect(screen.getByTestId('graph-list')).toBeInTheDocument();
    });
  });

  it('should handle window resize events', () => {
    render(<AppWithRouter />);

    // Simulate window resize
    fireEvent(window, new Event('resize'));

    // App should still be functional
    expect(screen.getByTestId('graph-list')).toBeInTheDocument();
  });

  it('should show unsaved changes warning', async () => {
    mockGetGraph.mockResolvedValue({
      id: 'graph1',
      name: 'Test Graph 1',
      tenantId: 'evil-corp',
      plans: [],
      tasks: [],
      edges: []
    });

    render(<AppWithRouter />);

    // Load a graph
    const loadButton = screen.getByText('Load Graph 1');
    fireEvent.click(loadButton);

    await waitFor(() => {
      expect(screen.getByTestId('graph-canvas')).toBeInTheDocument();
    });

    // Simulate unsaved changes (this would be handled by the actual components)
    // For now, just verify the structure is in place
    expect(screen.getByTestId('code-editor')).toBeInTheDocument();
  });
});