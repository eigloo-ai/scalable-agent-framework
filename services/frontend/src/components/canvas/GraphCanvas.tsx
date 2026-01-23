import React, { useRef, useState, useEffect } from 'react';
import './GraphCanvas.css';

export type ToolType = 'select' | 'task' | 'plan' | 'edge' | 'eraser';

export interface CanvasNode {
  id: string;
  type: 'task' | 'plan';
  x: number;
  y: number;
  width: number;
  height: number;
  label: string;
  selected: boolean;
}

export interface CanvasEdge {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  selected: boolean;
}

export interface GraphCanvasProps {
  graph?: any;
  selectedTool: ToolType;
  onToolChange: (tool: ToolType) => void;
  onNodeCreate?: (node: Omit<CanvasNode, 'id' | 'selected'>) => void;
  onEdgeCreate?: (edge: Omit<CanvasEdge, 'id' | 'selected'>) => void;
  onNodeSelect?: (nodeId: string) => void;
  onNodeDelete?: (nodeId: string) => void;
  onEdgeDelete?: (edgeId: string) => void;
  onNodeUpdate?: (nodeId: string, updates: Partial<CanvasNode>) => void;
}

const GraphCanvas: React.FC<GraphCanvasProps> = ({
  graph,
  selectedTool,
  onToolChange,
  onNodeCreate,
  onEdgeCreate,
  onNodeSelect,
  onNodeDelete,
  onEdgeDelete: _onEdgeDelete,
  onNodeUpdate
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  
  // Simple state management
  const [nodes, setNodes] = useState<CanvasNode[]>([]);
  const [edges, setEdges] = useState<CanvasEdge[]>([]);
  
  // Drawing state
  const [isDrawing, setIsDrawing] = useState(false);
  const [drawStart, setDrawStart] = useState({ x: 0, y: 0 });
  const [drawCurrent, setDrawCurrent] = useState({ x: 0, y: 0 });
  
  // Dragging state
  const [draggedNode, setDraggedNode] = useState<string | null>(null);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  
  // Edge drawing state
  const [edgeStart, setEdgeStart] = useState<string | null>(null);
  const [edgePreview, setEdgePreview] = useState<{ x: number; y: number } | null>(null);
  
  // Node editing state
  const [editingNode, setEditingNode] = useState<string | null>(null);
  const [editLabel, setEditLabel] = useState('');

  // Helper functions
  const getMousePos = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return { x: 0, y: 0 };
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top
    };
  };

  const findNodeAt = (x: number, y: number): CanvasNode | null => {
    return nodes.find(node => 
      x >= node.x && x <= node.x + node.width &&
      y >= node.y && y <= node.y + node.height
    ) || null;
  };

  const generateNodeId = () => `node_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  const generateEdgeId = (fromId: string, toId: string) => `${fromId}_to_${toId}`;

  // Drawing functions
  const drawNode = (ctx: CanvasRenderingContext2D, node: CanvasNode) => {
    // Set colors
    const fillColor = node.selected ? '#e3f2fd' : (node.type === 'plan' ? '#fff3e0' : '#e8f5e8');
    const strokeColor = node.selected ? '#2196f3' : (node.type === 'plan' ? '#ff9800' : '#4caf50');
    
    ctx.fillStyle = fillColor;
    ctx.strokeStyle = strokeColor;
    ctx.lineWidth = node.selected ? 3 : 2;
    
    if (node.type === 'plan') {
      // Draw circle for plan
      const centerX = node.x + node.width / 2;
      const centerY = node.y + node.height / 2;
      const radius = Math.min(node.width, node.height) / 2;
      
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
      ctx.fill();
      ctx.stroke();
    } else {
      // Draw rectangle for task
      ctx.fillRect(node.x, node.y, node.width, node.height);
      ctx.strokeRect(node.x, node.y, node.width, node.height);
    }
    
    // Draw label
    ctx.fillStyle = '#333';
    ctx.font = '12px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(node.label, node.x + node.width / 2, node.y + node.height / 2);
  };

  const drawEdge = (ctx: CanvasRenderingContext2D, edge: CanvasEdge) => {
    const fromNode = nodes.find(n => n.id === edge.fromNodeId);
    const toNode = nodes.find(n => n.id === edge.toNodeId);
    
    if (!fromNode || !toNode) return;
    
    const fromX = fromNode.x + fromNode.width / 2;
    const fromY = fromNode.y + fromNode.height / 2;
    const toX = toNode.x + toNode.width / 2;
    const toY = toNode.y + toNode.height / 2;
    
    ctx.strokeStyle = edge.selected ? '#2196f3' : '#666';
    ctx.lineWidth = edge.selected ? 3 : 2;
    
    ctx.beginPath();
    ctx.moveTo(fromX, fromY);
    ctx.lineTo(toX, toY);
    ctx.stroke();
    
    // Draw arrow
    const angle = Math.atan2(toY - fromY, toX - fromX);
    const arrowLength = 10;
    const arrowAngle = Math.PI / 6;
    
    ctx.beginPath();
    ctx.moveTo(toX, toY);
    ctx.lineTo(
      toX - arrowLength * Math.cos(angle - arrowAngle),
      toY - arrowLength * Math.sin(angle - arrowAngle)
    );
    ctx.moveTo(toX, toY);
    ctx.lineTo(
      toX - arrowLength * Math.cos(angle + arrowAngle),
      toY - arrowLength * Math.sin(angle + arrowAngle)
    );
    ctx.stroke();
  };

  const drawRubberBand = (ctx: CanvasRenderingContext2D) => {
    if (!isDrawing) return;
    
    const x = Math.min(drawStart.x, drawCurrent.x);
    const y = Math.min(drawStart.y, drawCurrent.y);
    const width = Math.abs(drawCurrent.x - drawStart.x);
    const height = Math.abs(drawCurrent.y - drawStart.y);
    
    ctx.strokeStyle = '#2196f3';
    ctx.lineWidth = 2;
    ctx.setLineDash([5, 5]);
    ctx.strokeRect(x, y, width, height);
    ctx.setLineDash([]);
  };

  const drawEdgePreview = (ctx: CanvasRenderingContext2D) => {
    if (!edgeStart || !edgePreview) return;
    
    const startNode = nodes.find(n => n.id === edgeStart);
    if (!startNode) return;
    
    const startX = startNode.x + startNode.width / 2;
    const startY = startNode.y + startNode.height / 2;
    
    ctx.strokeStyle = '#2196f3';
    ctx.lineWidth = 2;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.moveTo(startX, startY);
    ctx.lineTo(edgePreview.x, edgePreview.y);
    ctx.stroke();
    ctx.setLineDash([]);
  };

  const render = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Draw grid
    ctx.strokeStyle = '#f0f0f0';
    ctx.lineWidth = 1;
    const gridSize = 20;
    
    for (let x = 0; x < canvas.width; x += gridSize) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvas.height);
      ctx.stroke();
    }
    
    for (let y = 0; y < canvas.height; y += gridSize) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(canvas.width, y);
      ctx.stroke();
    }
    
    // Draw edges
    edges.forEach(edge => drawEdge(ctx, edge));
    
    // Draw nodes
    nodes.forEach(node => drawNode(ctx, node));
    
    // Draw rubber band
    drawRubberBand(ctx);
    
    // Draw edge preview
    drawEdgePreview(ctx);
  };

  // Event handlers
  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const pos = getMousePos(e);
    const clickedNode = findNodeAt(pos.x, pos.y);
    
    // Clear all selections first
    setNodes(prev => prev.map(node => ({ ...node, selected: false })));
    setEdges(prev => prev.map(edge => ({ ...edge, selected: false })));
    
    if (selectedTool === 'select') {
      if (clickedNode) {
        // Select and start dragging node
        setNodes(prev => prev.map(node => 
          node.id === clickedNode.id ? { ...node, selected: true } : node
        ));
        setDraggedNode(clickedNode.id);
        setDragOffset({
          x: pos.x - clickedNode.x,
          y: pos.y - clickedNode.y
        });
        onNodeSelect?.(clickedNode.id);
      }
    } else if (selectedTool === 'task' || selectedTool === 'plan') {
      // Start rubber band drawing
      setIsDrawing(true);
      setDrawStart(pos);
      setDrawCurrent(pos);
    } else if (selectedTool === 'edge') {
      if (clickedNode) {
        // Start edge drawing
        setEdgeStart(clickedNode.id);
        setEdgePreview(pos);
      }
    } else if (selectedTool === 'eraser') {
      if (clickedNode) {
        // Delete node
        setNodes(prev => prev.filter(node => node.id !== clickedNode.id));
        setEdges(prev => prev.filter(edge => 
          edge.fromNodeId !== clickedNode.id && edge.toNodeId !== clickedNode.id
        ));
        onNodeDelete?.(clickedNode.id);
      }
    }
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const pos = getMousePos(e);
    
    if (draggedNode) {
      // Move node
      const newX = pos.x - dragOffset.x;
      const newY = pos.y - dragOffset.y;
      
      setNodes(prev => prev.map(node => 
        node.id === draggedNode ? { ...node, x: newX, y: newY } : node
      ));
    } else if (isDrawing) {
      // Update rubber band
      setDrawCurrent(pos);
    } else if (edgeStart) {
      // Update edge preview
      setEdgePreview(pos);
    }
    
    render();
  };

  const handleMouseUp = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const pos = getMousePos(e);
    
    if (isDrawing && (selectedTool === 'task' || selectedTool === 'plan')) {
      // Create node from rubber band
      const x = Math.min(drawStart.x, drawCurrent.x);
      const y = Math.min(drawStart.y, drawCurrent.y);
      const width = Math.abs(drawCurrent.x - drawStart.x);
      const height = Math.abs(drawCurrent.y - drawStart.y);
      
      if (width > 20 && height > 20) {
        const nodeId = generateNodeId();
        const newNode: CanvasNode = {
          id: nodeId,
          type: selectedTool,
          x,
          y,
          width,
          height,
          label: `New ${selectedTool}`,
          selected: true
        };
        
        setNodes(prev => [...prev, newNode]);
        
        // Start editing the label immediately
        setEditingNode(nodeId);
        setEditLabel(newNode.label);
        
        // Notify parent
        onNodeCreate?.({
          type: selectedTool,
          x,
          y,
          width,
          height,
          label: newNode.label
        });
      }
    } else if (edgeStart) {
      // Complete edge drawing
      const targetNode = findNodeAt(pos.x, pos.y);
      if (targetNode && targetNode.id !== edgeStart) {
        const edgeId = generateEdgeId(edgeStart, targetNode.id);
        const newEdge: CanvasEdge = {
          id: edgeId,
          fromNodeId: edgeStart,
          toNodeId: targetNode.id,
          selected: false
        };
        
        // Check if edge already exists
        const existingEdge = edges.find(e => 
          e.fromNodeId === edgeStart && e.toNodeId === targetNode.id
        );
        
        if (!existingEdge) {
          setEdges(prev => [...prev, newEdge]);
          onEdgeCreate?.({
            fromNodeId: edgeStart,
            toNodeId: targetNode.id
          });
        }
      }
    }
    
    // Reset states
    setIsDrawing(false);
    setDraggedNode(null);
    setEdgeStart(null);
    setEdgePreview(null);
    
    render();
  };

  const handleDoubleClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const pos = getMousePos(e);
    const clickedNode = findNodeAt(pos.x, pos.y);
    
    if (clickedNode) {
      setEditingNode(clickedNode.id);
      setEditLabel(clickedNode.label);
    }
  };

  const handleLabelSubmit = () => {
    if (editingNode && editLabel.trim()) {
      const trimmedLabel = editLabel.trim();
      setNodes(prev => prev.map(node => 
        node.id === editingNode ? { ...node, label: trimmedLabel } : node
      ));
      onNodeUpdate?.(editingNode, { label: trimmedLabel });
    }
    setEditingNode(null);
    setEditLabel('');
    render();
  };

  const handleLabelCancel = () => {
    setEditingNode(null);
    setEditLabel('');
  };

  // Initialize nodes and edges from graph data
  useEffect(() => {
    if (!graph) return;

    const newNodes: CanvasNode[] = [];
    const newEdges: CanvasEdge[] = [];

    // Convert plans to nodes, preserving existing positions
    if (graph.plans) {
      graph.plans.forEach((plan: any, index: number) => {
        // Try to find existing node with same label to preserve position
        const existingNode = nodes.find(n => n.type === 'plan' && (n.label === plan.name || n.id === plan.name));
        
        const node: CanvasNode = {
          id: plan.name,
          type: 'plan',
          x: existingNode?.x ?? (100 + (index % 3) * 200),
          y: existingNode?.y ?? (100 + Math.floor(index / 3) * 150),
          width: existingNode?.width ?? 120,
          height: existingNode?.height ?? 120,
          label: plan.label || plan.name,
          selected: existingNode?.selected ?? false
        };
        newNodes.push(node);
      });
    }

    // Convert tasks to nodes, preserving existing positions
    if (graph.tasks) {
      graph.tasks.forEach((task: any, index: number) => {
        // Try to find existing node with same label to preserve position
        const existingNode = nodes.find(n => n.type === 'task' && (n.label === task.name || n.id === task.name));
        
        const node: CanvasNode = {
          id: task.name,
          type: 'task',
          x: existingNode?.x ?? (100 + (index % 3) * 200),
          y: existingNode?.y ?? (300 + Math.floor(index / 3) * 150),
          width: existingNode?.width ?? 140,
          height: existingNode?.height ?? 80,
          label: task.label || task.name,
          selected: existingNode?.selected ?? false
        };
        newNodes.push(node);
      });
    }

    // Convert relationships to edges
    if (graph.planToTasks) {
      Object.entries(graph.planToTasks).forEach(([planId, taskIds]: [string, any]) => {
        if (Array.isArray(taskIds)) {
          taskIds.forEach((taskId: string) => {
            newEdges.push({
              id: generateEdgeId(planId, taskId),
              fromNodeId: planId,
              toNodeId: taskId,
              selected: false
            });
          });
        }
      });
    }

    setNodes(newNodes);
    setEdges(newEdges);
  }, [graph]);

  // Render when state changes
  useEffect(() => {
    render();
  }, [nodes, edges, isDrawing, drawStart, drawCurrent, edgeStart, edgePreview]);

  // Set up canvas and resize handling
  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;
    
    const resizeCanvas = () => {
      const rect = container.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;
      render();
    };
    
    resizeCanvas();
    
    const handleResize = () => resizeCanvas();
    window.addEventListener('resize', handleResize);
    
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, []);

  return (
    <div className="graph-canvas" ref={containerRef} data-tool={selectedTool}>
      <div className="canvas-toolbar">
        <div className="toolbar-group">
          <button 
            className={`tool-button ${selectedTool === 'select' ? 'active' : ''}`}
            onClick={() => onToolChange('select')}
            title="Select and move nodes"
          >
            ↖️ Select
          </button>
          <button 
            className={`tool-button ${selectedTool === 'plan' ? 'active' : ''}`}
            onClick={() => onToolChange('plan')}
            title="Draw plan nodes (circles)"
          >
            ⭕ Plan
          </button>
          <button 
            className={`tool-button ${selectedTool === 'task' ? 'active' : ''}`}
            onClick={() => onToolChange('task')}
            title="Draw task nodes (rectangles)"
          >
            ⬜ Task
          </button>
          <button 
            className={`tool-button ${selectedTool === 'edge' ? 'active' : ''}`}
            onClick={() => onToolChange('edge')}
            title="Connect nodes with edges"
          >
            ↗️ Edge
          </button>
          <button 
            className={`tool-button ${selectedTool === 'eraser' ? 'active' : ''}`}
            onClick={() => onToolChange('eraser')}
            title="Delete nodes and edges"
          >
            🗑️ Eraser
          </button>
        </div>
      </div>
      
      <div className="canvas-content" style={{ position: 'relative' }}>
        <canvas
          ref={canvasRef}
          style={{ display: 'block', width: '100%', height: '100%' }}
          onMouseDown={handleMouseDown}
          onMouseMove={handleMouseMove}
          onMouseUp={handleMouseUp}
          onDoubleClick={handleDoubleClick}
        />
        
        {/* Node label editing overlay */}
        {editingNode && (() => {
          const node = nodes.find(n => n.id === editingNode);
          if (!node) return null;
          
          return (
            <div className="node-edit-overlay">
              <input
                type="text"
                value={editLabel}
                onChange={(e) => setEditLabel(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleLabelSubmit();
                  if (e.key === 'Escape') handleLabelCancel();
                }}
                onBlur={handleLabelSubmit}
                onFocus={(e) => e.target.select()}
                autoFocus
                className="node-edit-input"
                style={{
                  left: node.x + node.width / 2 - 40,
                  top: node.y + node.height / 2 - 10,
                  width: Math.max(80, node.width - 10)
                }}
              />
            </div>
          );
        })()}
      </div>
    </div>
  );
};

export default GraphCanvas;