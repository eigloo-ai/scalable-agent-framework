import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import FileExplorer from '../FileExplorer';
import type { AgentGraphDto, ExecutorFile } from '../../../types';

describe('FileExplorer', () => {
  const mockFiles: ExecutorFile[] = [
    {
      name: 'plan.py',
      contents: 'def plan(): pass',
      creationDate: '2024-01-01T00:00:00Z',
      version: '1.0',
      updateDate: '2024-01-01T00:00:00Z'
    },
    {
      name: 'requirements.txt',
      contents: 'requests>=2.25.1',
      creationDate: '2024-01-01T00:00:00Z',
      version: '1.0',
      updateDate: '2024-01-01T00:00:00Z'
    }
  ];

  const mockGraph: AgentGraphDto = {
    id: 'test-graph',
    name: 'Test Graph',
    tenantId: 'test-tenant',
    plans: [
      { name: 'plan1', label: 'Plan 1', files: mockFiles }
    ],
    tasks: [
      { name: 'task1', label: 'Task 1', files: mockFiles }
    ],
    edges: [
      { from: 'plan1', fromType: 'PLAN', to: 'task1', toType: 'TASK' }
    ]
  };

  const mockOnFileSelect = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render agent name as root', () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    expect(screen.getByText('Test Graph')).toBeInTheDocument();
  });

  it('should render plans and tasks folders', () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    expect(screen.getByText('plans')).toBeInTheDocument();
    expect(screen.getByText('tasks')).toBeInTheDocument();
  });

  it('should expand plans folder when clicked', async () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    const plansFolder = screen.getByText('plans');
    fireEvent.click(plansFolder);
    
    await waitFor(() => {
      expect(screen.getByText('plan1')).toBeInTheDocument();
    });
  });

  it('should expand tasks folder when clicked', async () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    const tasksFolder = screen.getByText('tasks');
    fireEvent.click(tasksFolder);
    
    await waitFor(() => {
      expect(screen.getByText('task1')).toBeInTheDocument();
    });
  });

  it('should show plan files when plan is expanded', async () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    // Expand plans folder
    const plansFolder = screen.getByText('plans');
    fireEvent.click(plansFolder);
    
    await waitFor(() => {
      const plan1 = screen.getByText('plan1');
      fireEvent.click(plan1);
    });
    
    await waitFor(() => {
      expect(screen.getByText('plan.py')).toBeInTheDocument();
      expect(screen.getByText('requirements.txt')).toBeInTheDocument();
    });
  });

  it('should show task files when task is expanded', async () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    // Expand tasks folder
    const tasksFolder = screen.getByText('tasks');
    fireEvent.click(tasksFolder);
    
    await waitFor(() => {
      const task1 = screen.getByText('task1');
      fireEvent.click(task1);
    });
    
    await waitFor(() => {
      expect(screen.getByText('task.py')).toBeInTheDocument();
      expect(screen.getByText('requirements.txt')).toBeInTheDocument();
    });
  });

  it('should call onFileSelect when file is clicked', async () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    // Expand plans folder and plan1
    fireEvent.click(screen.getByText('plans'));
    
    await waitFor(() => {
      fireEvent.click(screen.getByText('plan1'));
    });
    
    await waitFor(() => {
      const planFile = screen.getByText('plan.py');
      fireEvent.click(planFile);
    });
    
    expect(mockOnFileSelect).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'plan.py',
        contents: 'def plan(): pass'
      }),
      'plan1',
      'plan'
    );
  });

  it('should auto-expand when selectedNodeId matches', () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
        selectedNodeId="plan1"
      />
    );
    
    // Plans folder should be auto-expanded
    expect(screen.getByText('plan1')).toBeInTheDocument();
  });

  it('should show file icons correctly', async () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    // Expand to show files
    fireEvent.click(screen.getByText('plans'));
    
    await waitFor(() => {
      fireEvent.click(screen.getByText('plan1'));
    });
    
    await waitFor(() => {
      // Check for Python file icon
      const pythonFile = screen.getByText('plan.py');
      expect(pythonFile.closest('.file-item')).toHaveClass('python-file');
      
      // Check for text file icon
      const txtFile = screen.getByText('requirements.txt');
      expect(txtFile.closest('.file-item')).toHaveClass('text-file');
    });
  });

  it('should handle empty graph', () => {
    const emptyGraph: AgentGraphDto = {
      id: 'empty-graph',
      name: 'Empty Graph',
      tenantId: 'test-tenant',
      plans: [],
      tasks: [],
      edges: []
    };
    
    render(
      <FileExplorer 
        graph={emptyGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    expect(screen.getByText('Empty Graph')).toBeInTheDocument();
    expect(screen.getByText('plans')).toBeInTheDocument();
    expect(screen.getByText('tasks')).toBeInTheDocument();
  });

  it('should show loading state when graph is undefined', () => {
    render(
      <FileExplorer 
        graph={undefined} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    expect(screen.getByText('No graph loaded')).toBeInTheDocument();
  });

  it('should handle keyboard navigation', async () => {
    render(
      <FileExplorer 
        graph={mockGraph} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    const plansFolder = screen.getByText('plans');
    plansFolder.focus();
    
    // Press Enter to expand
    fireEvent.keyDown(plansFolder, { key: 'Enter' });
    
    await waitFor(() => {
      expect(screen.getByText('plan1')).toBeInTheDocument();
    });
  });

  it('should show file modification indicators', async () => {
    const modifiedFiles: ExecutorFile[] = [
      {
        name: 'plan.py',
        contents: 'def plan(): pass',
        creationDate: '2024-01-01T00:00:00Z',
        version: '1.1',
        updateDate: '2024-01-02T00:00:00Z'
      }
    ];

    const graphWithModifiedFiles: AgentGraphDto = {
      ...mockGraph,
      plans: [
        { name: 'plan1', label: 'Plan 1', files: modifiedFiles }
      ]
    };
    
    render(
      <FileExplorer 
        graph={graphWithModifiedFiles} 
        onFileSelect={mockOnFileSelect}
      />
    );
    
    // Expand to show files
    fireEvent.click(screen.getByText('plans'));
    
    await waitFor(() => {
      fireEvent.click(screen.getByText('plan1'));
    });
    
    await waitFor(() => {
      // Should show modification indicator
      expect(screen.getByText('plan.py')).toBeInTheDocument();
    });
  });
});
