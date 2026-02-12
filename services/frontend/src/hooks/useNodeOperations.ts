import { useCallback } from 'react';
import { useAppContext } from './useAppContext';
import { useGraphs } from './useGraphs';
import { useValidation } from './useValidation';
import { createDefaultFilesForPlan, createDefaultFilesForTask } from '../utils/templateGenerator';
import type { AgentGraphDto, GraphEdgeDto, PlanDto, TaskDto } from '../types';

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

export function useNodeOperations() {
  const { state, dispatch } = useAppContext();
  const { updateGraph } = useGraphs();
  const { validateNodeName, validateGraph } = useValidation();

  const addPlanNode = useCallback(async (planName: string, label?: string) => {
    if (!state.currentGraph) {
      throw new Error('No graph loaded');
    }

    const existingNodeNames = [
      ...state.currentGraph.plans.map(p => p.name),
      ...state.currentGraph.tasks.map(t => t.name)
    ];

    const validationResult = await validateNodeName(
      planName,
      state.currentGraph.id,
      existingNodeNames
    );

    if (!validationResult.valid) {
      throw new Error(validationResult.errors.join(', '));
    }

    const newPlan: PlanDto = {
      name: planName,
      label: label || planName,
      files: createDefaultFilesForPlan(planName)
    };

    const updatedGraph: AgentGraphDto = canonicalizeGraphEdges({
      ...state.currentGraph,
      plans: [...state.currentGraph.plans, newPlan],
      edges: [...(state.currentGraph.edges || [])],
      updatedAt: new Date().toISOString()
    });

    try {
      await updateGraph(state.currentGraph.id, updatedGraph);
      return newPlan;
    } catch (error) {
      dispatch({
        type: 'SET_ERROR',
        payload: error instanceof Error ? error.message : 'Failed to add plan node'
      });
      throw error;
    }
  }, [state.currentGraph, updateGraph, dispatch, validateNodeName]);

  const addTaskNode = useCallback(async (taskName: string, label?: string) => {
    if (!state.currentGraph) {
      throw new Error('No graph loaded');
    }

    const existingNodeNames = [
      ...state.currentGraph.plans.map(p => p.name),
      ...state.currentGraph.tasks.map(t => t.name)
    ];

    const validationResult = await validateNodeName(
      taskName,
      state.currentGraph.id,
      existingNodeNames
    );

    if (!validationResult.valid) {
      throw new Error(validationResult.errors.join(', '));
    }

    const newTask: TaskDto = {
      name: taskName,
      label: label || taskName,
      files: createDefaultFilesForTask(taskName)
    };

    const updatedGraph: AgentGraphDto = canonicalizeGraphEdges({
      ...state.currentGraph,
      tasks: [...state.currentGraph.tasks, newTask],
      edges: [...(state.currentGraph.edges || [])],
      updatedAt: new Date().toISOString()
    });

    try {
      await updateGraph(state.currentGraph.id, updatedGraph);
      return newTask;
    } catch (error) {
      dispatch({
        type: 'SET_ERROR',
        payload: error instanceof Error ? error.message : 'Failed to add task node'
      });
      throw error;
    }
  }, [state.currentGraph, updateGraph, dispatch, validateNodeName]);

  const removeNode = useCallback(async (nodeId: string, nodeType: 'plan' | 'task') => {
    if (!state.currentGraph) {
      throw new Error('No graph loaded');
    }

    const updatedGraph: AgentGraphDto = canonicalizeGraphEdges({
      ...state.currentGraph,
      plans: nodeType === 'plan'
        ? state.currentGraph.plans.filter(p => p.name !== nodeId)
        : state.currentGraph.plans,
      tasks: nodeType === 'task'
        ? state.currentGraph.tasks.filter(t => t.name !== nodeId)
        : state.currentGraph.tasks,
      edges: (state.currentGraph.edges || []).filter(edge => edge.from !== nodeId && edge.to !== nodeId),
      updatedAt: new Date().toISOString()
    });

    try {
      await updateGraph(state.currentGraph.id, updatedGraph);
    } catch (error) {
      dispatch({
        type: 'SET_ERROR',
        payload: error instanceof Error ? error.message : 'Failed to remove node'
      });
      throw error;
    }
  }, [state.currentGraph, updateGraph, dispatch]);

  const connectNodes = useCallback(async (fromNodeId: string, toNodeId: string, fromType: 'plan' | 'task', toType: 'plan' | 'task') => {
    if (!state.currentGraph) {
      throw new Error('No graph loaded');
    }

    if (fromType === toType) {
      throw new Error('Cannot connect nodes of the same type');
    }

    const newEdge: GraphEdgeDto = fromType === 'plan'
      ? { from: fromNodeId, fromType: 'PLAN', to: toNodeId, toType: 'TASK' }
      : { from: fromNodeId, fromType: 'TASK', to: toNodeId, toType: 'PLAN' };

    let updatedGraph: AgentGraphDto = canonicalizeGraphEdges({
      ...state.currentGraph,
      edges: dedupeEdges([...(state.currentGraph.edges || []), newEdge]),
      updatedAt: new Date().toISOString()
    });

    const graphValidationResult = await validateGraph(updatedGraph);
    if (!graphValidationResult.valid) {
      throw new Error(`Connection validation failed: ${graphValidationResult.errors.join(', ')}`);
    }

    try {
      await updateGraph(state.currentGraph.id, updatedGraph);
    } catch (error) {
      dispatch({
        type: 'SET_ERROR',
        payload: error instanceof Error ? error.message : 'Failed to connect nodes'
      });
      throw error;
    }
  }, [state.currentGraph, updateGraph, dispatch, validateGraph]);

  return {
    addPlanNode,
    addTaskNode,
    removeNode,
    connectNodes
  };
}
