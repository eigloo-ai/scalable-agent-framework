import React, { useCallback, useState, useEffect } from 'react';
import ThreePaneLayout from '../components/layout/ThreePaneLayout';
import AppHeader from '../components/layout/AppHeader';
import FileExplorer from '../components/file-explorer/FileExplorer';
import GraphCanvas, { type ToolType, type CanvasNode, type CanvasEdge } from '../components/canvas/GraphCanvas';
import CodeEditor from '../components/editor/CodeEditor';
import { useAppContext } from '../hooks/useAppContext';
import { graphApi } from '../api/client';
import { createDefaultFilesForPlan, createDefaultFilesForTask } from '../utils/templateGenerator';
import type { AgentGraphDto, GraphEdgeDto } from '../types';

const edgeKey = (edge: GraphEdgeDto): string =>
  `${edge.from}|${edge.fromType}|${edge.to}|${edge.toType}`;

const dedupeEdges = (edges: GraphEdgeDto[]): GraphEdgeDto[] => {
  const deduped = new Map<string, GraphEdgeDto>();
  edges.forEach(edge => {
    if (!edge?.from || !edge?.to || !edge?.fromType || !edge?.toType) {
      return;
    }
    deduped.set(edgeKey(edge), edge);
  });
  return Array.from(deduped.values());
};

const canonicalizeGraphEdges = (graph: AgentGraphDto): AgentGraphDto => {
  return {
    ...graph,
    edges: dedupeEdges(graph.edges || [])
  };
};

const GraphEditor: React.FC = () => {
  const { state, dispatch } = useAppContext();
  const { selectedFile, currentGraph } = state;
  const [selectedTool, setSelectedTool] = useState<ToolType>('select');

  // Determine language based on file extension
  const getLanguage = (fileName: string): 'python' | 'plaintext' => {
    return fileName.endsWith('.py') ? 'python' : 'plaintext';
  };

  // Handle file content changes
  const handleFileChange = useCallback((content: string) => {
    if (!selectedFile) return;
    
    // Update the file content in the current graph and selected file
    dispatch({ 
      type: 'UPDATE_FILE_CONTENT', 
      payload: { fileName: selectedFile.name, content } 
    });
  }, [selectedFile, dispatch]);

  // Handle complete graph save (replaces individual file saves)
  const handleGraphSave = useCallback(async () => {
    console.log('handleGraphSave called');
    console.log('currentGraph:', currentGraph);
    console.log('currentGraph.id:', currentGraph?.id);
    
    if (!currentGraph || !currentGraph.id) {
      console.log('Early return: currentGraph or currentGraph.id is falsy');
      return;
    }

    try {
      dispatch({ type: 'SET_LOADING', payload: true });
      dispatch({ type: 'CLEAR_ERROR' });

      // Validate the graph before saving
      const validatedGraph = canonicalizeGraphEdges({
        ...currentGraph,
        plans: [...(currentGraph.plans || [])],
        tasks: [...(currentGraph.tasks || [])],
        edges: [...(currentGraph.edges || [])]
      });
      console.log('Validated graph:', validatedGraph);
      
      // Use POST for new graphs, PUT for existing graphs
      let updatedGraph: AgentGraphDto;
      if (currentGraph.id.startsWith('new-')) {
        // Create new graph with complete data
        console.log('Creating new graph with complete data:', validatedGraph);
        updatedGraph = await graphApi.createGraph(validatedGraph);
      } else {
        // Update existing graph
        console.log('Updating existing graph');
        updatedGraph = await graphApi.updateGraph(validatedGraph.id, validatedGraph);
      }
      
      // Update the current graph with the response from server
      dispatch({ type: 'SET_CURRENT_GRAPH', payload: updatedGraph });
      
      console.log('Graph saved successfully');
    } catch (error) {
      console.error('Failed to save graph:', error);
      dispatch({ 
        type: 'SET_ERROR', 
        payload: 'Failed to save graph. Please try again.' 
      });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  }, [currentGraph, dispatch]);

  // Handle tool changes
  const handleToolChange = useCallback((tool: ToolType) => {
    setSelectedTool(tool);
  }, []);

  // Handle node creation with template files
  const handleNodeCreate = useCallback((node: Omit<CanvasNode, 'id' | 'selected'>) => {
    console.log('Node creation requested:', node);
    
    if (!currentGraph) return;
    

    
    // Create a completely new graph object to ensure React detects the change
    const updatedGraph = {
      ...currentGraph,
      plans: [...(currentGraph.plans || [])],
      tasks: [...(currentGraph.tasks || [])],
      edges: [...(currentGraph.edges || [])]
    };
    
    if (node.type === 'plan') {
      // Create template files for the plan
      const templateFiles = createDefaultFilesForPlan(node.label);
      
      // Add new plan to the graph
      const newPlan = {
        name: node.label,
        label: node.label,
        files: templateFiles
      };
      updatedGraph.plans.push(newPlan);
      console.log('Added new plan with template files:', newPlan);
    } else if (node.type === 'task') {
      // Create template files for the task
      const templateFiles = createDefaultFilesForTask(node.label);
      
      // Add new task to the graph
      const newTask = {
        name: node.label,
        label: node.label,
        files: templateFiles
      };
      updatedGraph.tasks.push(newTask);
      console.log('Added new task with template files:', newTask);
    }
    
    // Update the current graph in state
    dispatch({ type: 'SET_CURRENT_GRAPH', payload: updatedGraph });
  }, [currentGraph, dispatch]);

  // Handle edge creation
  const handleEdgeCreate = useCallback((edge: Omit<CanvasEdge, 'id' | 'selected'>) => {
    console.log('Edge creation requested:', edge);
    
    if (!currentGraph) return;
    
    // Create a completely new graph object to ensure React detects the change
    const updatedGraph = {
      ...currentGraph,
      plans: [...(currentGraph.plans || [])],
      tasks: [...(currentGraph.tasks || [])],
      edges: [...(currentGraph.edges || [])]
    };
    
    // Find the from and to nodes
    const fromNode = [...updatedGraph.plans, ...updatedGraph.tasks].find(n => n.name === edge.fromNodeId);
    const toNode = [...updatedGraph.plans, ...updatedGraph.tasks].find(n => n.name === edge.toNodeId);
    
    if (!fromNode || !toNode) return;
    
    // Determine the relationship type based on node types
    const fromIsPlan = updatedGraph.plans.some(p => p.name === edge.fromNodeId);
    const toIsPlan = updatedGraph.plans.some(p => p.name === edge.toNodeId);
    
    if (fromIsPlan && !toIsPlan) {
      updatedGraph.edges = dedupeEdges([
        ...updatedGraph.edges,
        { from: edge.fromNodeId, fromType: 'PLAN', to: edge.toNodeId, toType: 'TASK' }
      ]);
    } else if (!fromIsPlan && toIsPlan) {
      updatedGraph.edges = dedupeEdges([
        ...updatedGraph.edges,
        { from: edge.fromNodeId, fromType: 'TASK', to: edge.toNodeId, toType: 'PLAN' }
      ]);
    }
    
    // Update the current graph in state
    dispatch({ type: 'SET_CURRENT_GRAPH', payload: canonicalizeGraphEdges(updatedGraph) });
  }, [currentGraph, dispatch]);

  // Handle node selection
  const handleNodeSelect = useCallback((nodeId: string) => {
    console.log('Node selected:', nodeId);
    // TODO: Expand corresponding folder in file explorer
  }, []);

  // Handle node deletion
  const handleNodeDelete = useCallback((nodeId: string) => {
    console.log('Node deletion requested:', nodeId);
    
    if (!currentGraph) return;
    
    // Create a completely new graph object to ensure React detects the change
    const updatedGraph = {
      ...currentGraph,
      plans: [...(currentGraph.plans || [])],
      tasks: [...(currentGraph.tasks || [])],
      edges: [...(currentGraph.edges || [])]
    };
    
    // Remove the node from plans or tasks
    updatedGraph.plans = updatedGraph.plans.filter(plan => plan.name !== nodeId);
    updatedGraph.tasks = updatedGraph.tasks.filter(task => task.name !== nodeId);
    
    // Remove edges attached to the deleted node.
    updatedGraph.edges = updatedGraph.edges.filter(edge =>
      edge.from !== nodeId && edge.to !== nodeId
    );
    
    // Update the current graph in state
    dispatch({ type: 'SET_CURRENT_GRAPH', payload: canonicalizeGraphEdges(updatedGraph) });
  }, [currentGraph, dispatch]);

  // Handle edge deletion (placeholder - will be implemented in later tasks)
  const handleEdgeDelete = useCallback((edgeId: string) => {
    console.log('Edge deletion requested:', edgeId);
    // TODO: Implement edge deletion in backend integration
  }, []);



  // Handle node updates (label changes, etc.)
  const handleNodeUpdate = useCallback((nodeId: string, updates: Partial<CanvasNode>) => {
    if (updates.label && currentGraph) {
      const newName = updates.label.trim();
      if (!newName || newName === nodeId) return; // No change or empty name
      
      // Create a completely new graph object to ensure React detects the change
      const updatedGraph = {
        ...currentGraph,
        plans: [...(currentGraph.plans || [])],
        tasks: [...(currentGraph.tasks || [])],
        edges: [...(currentGraph.edges || [])]
      };
      
      // Check if it's a plan or task being updated
      const isPlan = updatedGraph.plans.some(p => p.name === nodeId);
      const isTask = updatedGraph.tasks.some(t => t.name === nodeId);
      
      if (isPlan) {
        // Update plan name and label
        updatedGraph.plans = updatedGraph.plans.map((plan: any) => 
          plan.name === nodeId ? { ...plan, name: newName, label: newName } : plan
        );
        
      } else if (isTask) {
        // Update task name and label
        updatedGraph.tasks = updatedGraph.tasks.map((task: any) => 
          task.name === nodeId ? { ...task, name: newName, label: newName } : task
        );
      }

      updatedGraph.edges = updatedGraph.edges.map(edge => ({
        ...edge,
        from: edge.from === nodeId ? newName : edge.from,
        to: edge.to === nodeId ? newName : edge.to
      }));
      
      // Update the current graph in state
      dispatch({ type: 'SET_CURRENT_GRAPH', payload: canonicalizeGraphEdges(updatedGraph) });
      
      console.log(`Updated node ${nodeId} to ${newName}`);
    }
  }, [currentGraph, dispatch]);

  // Initialize currentGraph when component mounts (for new graphs)
  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const graphName = searchParams.get('name');
    
    if (graphName && (!currentGraph || currentGraph.name !== graphName)) {
      // Create a new graph structure for the new graph
      const newGraph = {
        id: `new-${Date.now()}`,
        name: graphName,
        tenantId: 'evil-corp', // Default tenant
        status: 'NEW' as const,
        plans: [],
        tasks: [],
        edges: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      
      console.log('Initializing new graph:', newGraph);
      dispatch({ type: 'SET_CURRENT_GRAPH', payload: newGraph });
    }
  }, [currentGraph, dispatch]);

  // Debug: Log when currentGraph changes
  useEffect(() => {
    console.log('GraphEditor: currentGraph changed:', currentGraph);
  }, [currentGraph]);

  return (
    <ThreePaneLayout
      header={
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <AppHeader />
          {currentGraph && (
            <button 
              onClick={() => {
                console.log('Save Graph button clicked');
                handleGraphSave();
              }}
              disabled={state.isLoading}
              style={{
                padding: '0.5rem 1rem',
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: state.isLoading ? 'not-allowed' : 'pointer',
                fontSize: '14px',
                fontWeight: 'bold'
              }}
            >
              {state.isLoading ? 'Saving...' : 'Save Graph'}
            </button>
          )}
        </div>
      }
      leftPane={<FileExplorer />}
      centerTopPane={
        <GraphCanvas
          graph={currentGraph || undefined}
          selectedTool={selectedTool}
          onToolChange={handleToolChange}
          onNodeCreate={handleNodeCreate}
          onEdgeCreate={handleEdgeCreate}
          onNodeSelect={handleNodeSelect}
          onNodeDelete={handleNodeDelete}
          onEdgeDelete={handleEdgeDelete}

          onNodeUpdate={handleNodeUpdate}
        />
      }
      centerBottomPane={
        <CodeEditor
          file={selectedFile || undefined}
          language={selectedFile ? getLanguage(selectedFile.name) : 'python'}
          onChange={handleFileChange}
          onSave={handleGraphSave}
        />
      }
    />
  );
};

export default GraphEditor;
