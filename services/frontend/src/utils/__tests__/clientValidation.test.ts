import { ClientValidation } from '../clientValidation';
import type { AgentGraphDto } from '../../types';

describe('ClientValidation', () => {
  describe('validatePythonIdentifier', () => {
    it('should validate valid Python identifiers', () => {
      const validNames = ['valid_name', 'ValidName', '_private', 'name123', 'a'];
      
      validNames.forEach(name => {
        const result = ClientValidation.validatePythonIdentifier(name);
        expect(result.valid).toBe(true);
        expect(result.errors).toHaveLength(0);
      });
    });

    it('should reject invalid Python identifiers', () => {
      const invalidNames = ['123invalid', 'invalid-name', 'invalid name', 'invalid.name', ''];
      
      invalidNames.forEach(name => {
        const result = ClientValidation.validatePythonIdentifier(name);
        expect(result.valid).toBe(false);
        expect(result.errors.length).toBeGreaterThan(0);
      });
    });

    it('should reject Python keywords', () => {
      const keywords = ['def', 'class', 'if', 'else', 'for', 'while', 'import'];
      
      keywords.forEach(keyword => {
        const result = ClientValidation.validatePythonIdentifier(keyword);
        expect(result.valid).toBe(false);
        expect(result.errors.some(error => error.includes('Python keyword'))).toBe(true);
      });
    });

    it('should warn about naming conventions', () => {
      const result1 = ClientValidation.validatePythonIdentifier('_private');
      expect(result1.valid).toBe(true);
      expect(result1.warnings.some(warning => warning.includes('underscore'))).toBe(true);

      const result2 = ClientValidation.validatePythonIdentifier('CONSTANT');
      expect(result2.valid).toBe(true);
      expect(result2.warnings.some(warning => warning.includes('uppercase'))).toBe(true);
    });
  });

  describe('validateNodeNameUniqueness', () => {
    it('should allow unique names', () => {
      const result = ClientValidation.validateNodeNameUniqueness('new_name', ['existing1', 'existing2']);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should reject duplicate names', () => {
      const result = ClientValidation.validateNodeNameUniqueness('existing1', ['existing1', 'existing2']);
      expect(result.valid).toBe(false);
      expect(result.errors.some(error => error.includes('already exists'))).toBe(true);
    });

    it('should allow name when excluding current node', () => {
      const result = ClientValidation.validateNodeNameUniqueness('existing1', ['existing1', 'existing2'], 'existing1');
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });
  });

  describe('validateNodeName', () => {
    it('should combine identifier and uniqueness validation', () => {
      // Valid name that's unique
      const result1 = ClientValidation.validateNodeName('valid_name', ['other_name']);
      expect(result1.valid).toBe(true);

      // Invalid identifier
      const result2 = ClientValidation.validateNodeName('123invalid', ['other_name']);
      expect(result2.valid).toBe(false);
      expect(result2.errors.some(error => error.includes('valid Python identifier'))).toBe(true);

      // Valid identifier but duplicate
      const result3 = ClientValidation.validateNodeName('other_name', ['other_name']);
      expect(result3.valid).toBe(false);
      expect(result3.errors.some(error => error.includes('already exists'))).toBe(true);
    });
  });

  describe('validateGraph', () => {
    it('should validate a simple valid graph', () => {
      const graph: AgentGraphDto = {
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

      const result = ClientValidation.validateGraph(graph);
      expect(result.valid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should detect duplicate node names', () => {
      const graph: AgentGraphDto = {
        id: 'test-graph',
        name: 'Test Graph',
        tenantId: 'test-tenant',
        plans: [
          { name: 'duplicate', label: 'Plan 1' }
        ],
        tasks: [
          { name: 'duplicate', label: 'Task 1' }
        ],
        edges: []
      };

      const result = ClientValidation.validateGraph(graph);
      expect(result.valid).toBe(false);
      expect(result.errors.some(error => error.includes('used multiple times'))).toBe(true);
    });

    it('should detect orphaned nodes', () => {
      const graph: AgentGraphDto = {
        id: 'test-graph',
        name: 'Test Graph',
        tenantId: 'test-tenant',
        plans: [
          { name: 'orphan_plan', label: 'Orphan Plan' }
        ],
        tasks: [],
        edges: []
      };

      const result = ClientValidation.validateGraph(graph);
      expect(result.valid).toBe(true); // Orphaned nodes are warnings, not errors
      expect(result.warnings.length).toBeGreaterThan(0);
      expect(result.warnings.some(warning => warning.includes('not connected'))).toBe(true);
    });
  });

  describe('performance', () => {
    it('should validate quickly without API calls', () => {
      const startTime = performance.now();
      
      // Run validation 100 times
      for (let i = 0; i < 100; i++) {
        ClientValidation.validateNodeName(`test_name_${i}`, ['other_name']);
      }
      
      const endTime = performance.now();
      const duration = endTime - startTime;
      
      // Should complete in under 10ms for 100 validations
      expect(duration).toBeLessThan(10);
    });
  });
});
