import type { AgentGraphDto, GraphEdgeDto, ValidationResult } from '../types';

// Python identifier pattern: starts with letter or underscore, followed by letters, digits, or underscores
const PYTHON_IDENTIFIER_PATTERN = /^[a-zA-Z_][a-zA-Z0-9_]*$/;

// Python keywords that cannot be used as identifiers
const PYTHON_KEYWORDS = new Set([
  'False', 'None', 'True', 'and', 'as', 'assert', 'async', 'await', 'break', 'class',
  'continue', 'def', 'del', 'elif', 'else', 'except', 'finally', 'for', 'from',
  'global', 'if', 'import', 'in', 'is', 'lambda', 'nonlocal', 'not', 'or', 'pass',
  'raise', 'return', 'try', 'while', 'with', 'yield'
]);

function deriveCanonicalEdges(graph: AgentGraphDto): GraphEdgeDto[] {
  const deduped = new Map<string, GraphEdgeDto>();

  (graph.edges || []).forEach(edge => {
    if (!edge) return;
    const key = `${edge.from}|${edge.fromType}|${edge.to}|${edge.toType}`;
    deduped.set(key, edge);
  });

  return Array.from(deduped.values());
}

/**
 * Client-side validation utilities for graph composer.
 */
export class ClientValidation {

  static validatePythonIdentifier(nodeName: string): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!nodeName || nodeName.trim().length === 0) {
      errors.push('Node name cannot be empty');
      return { valid: false, errors, warnings };
    }

    const trimmedName = nodeName.trim();

    if (!PYTHON_IDENTIFIER_PATTERN.test(trimmedName)) {
      errors.push(
        `Node name '${trimmedName}' is not a valid Python identifier. ` +
        'It must start with a letter or underscore and contain only letters, digits, and underscores.'
      );
    }

    if (PYTHON_KEYWORDS.has(trimmedName)) {
      errors.push(`Node name '${trimmedName}' is a Python keyword and cannot be used as an identifier`);
    }

    if (trimmedName.startsWith('_')) {
      warnings.push(`Node name '${trimmedName}' starts with underscore, which is typically reserved for internal use`);
    }

    if (trimmedName.toUpperCase() === trimmedName && trimmedName.length > 1) {
      warnings.push(`Node name '${trimmedName}' is all uppercase, which is typically reserved for constants`);
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  static validateNodeNameUniqueness(
    nodeName: string,
    existingNodeNames: string[],
    excludeNodeId?: string
  ): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!nodeName || nodeName.trim().length === 0) {
      errors.push('Node name cannot be empty');
      return { valid: false, errors, warnings };
    }

    const trimmedName = nodeName.trim();
    const relevantNames = excludeNodeId
      ? existingNodeNames.filter(name => name !== excludeNodeId)
      : existingNodeNames;

    if (relevantNames.includes(trimmedName)) {
      errors.push(`Node name '${trimmedName}' already exists in the graph`);
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  static validateNodeName(
    nodeName: string,
    existingNodeNames: string[] = [],
    excludeNodeId?: string
  ): ValidationResult {
    const identifierResult = this.validatePythonIdentifier(nodeName);
    if (!identifierResult.valid) {
      return identifierResult;
    }

    const uniquenessResult = this.validateNodeNameUniqueness(nodeName, existingNodeNames, excludeNodeId);
    return {
      valid: identifierResult.valid && uniquenessResult.valid,
      errors: [...identifierResult.errors, ...uniquenessResult.errors],
      warnings: [...identifierResult.warnings, ...uniquenessResult.warnings]
    };
  }

  static validateConnectionConstraints(graph: AgentGraphDto): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    const planNames = new Set(graph.plans?.map(p => p.name) || []);
    const taskNames = new Set(graph.tasks?.map(t => t.name) || []);

    for (const edge of deriveCanonicalEdges(graph)) {
      if (!edge.from || !edge.to) {
        errors.push('Edge endpoints cannot be empty');
        continue;
      }
      if (edge.fromType === edge.toType) {
        errors.push(`Edge '${edge.from}' -> '${edge.to}' must connect PLAN to TASK or TASK to PLAN`);
        continue;
      }
      if (edge.fromType === 'PLAN' && !planNames.has(edge.from)) {
        errors.push(`Plan '${edge.from}' referenced in edges but not found in graph`);
      }
      if (edge.fromType === 'TASK' && !taskNames.has(edge.from)) {
        errors.push(`Task '${edge.from}' referenced in edges but not found in graph`);
      }
      if (edge.toType === 'PLAN' && !planNames.has(edge.to)) {
        errors.push(`Plan '${edge.to}' referenced in edges but not found in graph`);
      }
      if (edge.toType === 'TASK' && !taskNames.has(edge.to)) {
        errors.push(`Task '${edge.to}' referenced in edges but not found in graph`);
      }
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  static validateTaskUpstreamConstraints(graph: AgentGraphDto): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];
    const incomingCounts = new Map<string, number>();

    deriveCanonicalEdges(graph).forEach(edge => {
      if (edge.fromType === 'PLAN' && edge.toType === 'TASK') {
        incomingCounts.set(edge.to, (incomingCounts.get(edge.to) || 0) + 1);
      }
    });

    (graph.tasks || []).forEach(task => {
      const incoming = incomingCounts.get(task.name) || 0;
      if (incoming !== 1) {
        errors.push(`Task '${task.name}' must have exactly one upstream plan`);
      }
    });

    return { valid: errors.length === 0, errors, warnings };
  }

  static validatePlanUpstreamConstraints(graph: AgentGraphDto): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];
    const taskNames = new Set(graph.tasks?.map(t => t.name) || []);

    deriveCanonicalEdges(graph).forEach(edge => {
      if (edge.fromType === 'TASK' && edge.toType === 'PLAN' && !taskNames.has(edge.from)) {
        errors.push(`Plan '${edge.to}' references upstream task '${edge.from}' which does not exist`);
      }
    });

    return { valid: errors.length === 0, errors, warnings };
  }

  static validateConnectivity(graph: AgentGraphDto): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];
    const allNodes = new Set<string>();
    const connectedNodes = new Set<string>();

    graph.plans?.forEach(plan => allNodes.add(plan.name));
    graph.tasks?.forEach(task => allNodes.add(task.name));

    deriveCanonicalEdges(graph).forEach(edge => {
      connectedNodes.add(edge.from);
      connectedNodes.add(edge.to);
    });

    Array.from(allNodes)
      .filter(node => !connectedNodes.has(node))
      .forEach(node => warnings.push(`Node '${node}' is not connected to any other nodes`));

    if (allNodes.size === 0) {
      warnings.push('Graph contains no nodes');
    } else if (allNodes.size === 1) {
      warnings.push('Graph contains only one node');
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  static validateGraphNodeNameUniqueness(graph: AgentGraphDto): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    const allNodeNames = new Set<string>();
    const duplicateNames = new Set<string>();

    graph.plans?.forEach(plan => {
      if (!plan.name) return;
      if (allNodeNames.has(plan.name)) duplicateNames.add(plan.name);
      else allNodeNames.add(plan.name);
    });

    graph.tasks?.forEach(task => {
      if (!task.name) return;
      if (allNodeNames.has(task.name)) duplicateNames.add(task.name);
      else allNodeNames.add(task.name);
    });

    duplicateNames.forEach(name => {
      errors.push(`Node name '${name}' is used multiple times in the graph`);
    });

    return { valid: errors.length === 0, errors, warnings };
  }

  static validateGraph(graph: AgentGraphDto): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!graph) {
      errors.push('Graph cannot be null');
      return { valid: false, errors, warnings };
    }

    if (!graph.name || graph.name.trim().length === 0) {
      errors.push('Graph name cannot be empty');
    }

    if (!graph.tenantId || graph.tenantId.trim().length === 0) {
      errors.push('Tenant ID cannot be empty');
    }

    const validationChecks = [
      this.validateGraphNodeNameUniqueness(graph),
      this.validateConnectionConstraints(graph),
      this.validateTaskUpstreamConstraints(graph),
      this.validatePlanUpstreamConstraints(graph),
      this.validateConnectivity(graph)
    ];

    validationChecks.forEach(result => {
      errors.push(...result.errors);
      warnings.push(...result.warnings);
    });

    return { valid: errors.length === 0, errors, warnings };
  }
}
