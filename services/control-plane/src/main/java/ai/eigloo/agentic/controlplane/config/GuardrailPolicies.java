package ai.eigloo.agentic.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration properties for guardrail policies.
 * 
 * This class holds the configuration for various guardrails including:
 * - Token limits
 * - Cost thresholds
 * - Iteration caps
 * - Execution timeouts
 */
@Component
@ConfigurationProperties(prefix = "agentic.guardrails")
public class GuardrailPolicies {
    
    private TokenLimits tokenLimits = new TokenLimits();
    private CostThresholds costThresholds = new CostThresholds();
    private IterationCaps iterationCaps = new IterationCaps();
    private ExecutionTimeouts executionTimeouts = new ExecutionTimeouts();
    private Map<String, TenantPolicies> tenantPolicies = Map.of();
    
    // Getters and setters
    public TokenLimits getTokenLimits() { return tokenLimits; }
    public void setTokenLimits(TokenLimits tokenLimits) { this.tokenLimits = tokenLimits; }
    
    public CostThresholds getCostThresholds() { return costThresholds; }
    public void setCostThresholds(CostThresholds costThresholds) { this.costThresholds = costThresholds; }
    
    public IterationCaps getIterationCaps() { return iterationCaps; }
    public void setIterationCaps(IterationCaps iterationCaps) { this.iterationCaps = iterationCaps; }
    
    public ExecutionTimeouts getExecutionTimeouts() { return executionTimeouts; }
    public void setExecutionTimeouts(ExecutionTimeouts executionTimeouts) { this.executionTimeouts = executionTimeouts; }
    
    public Map<String, TenantPolicies> getTenantPolicies() { return tenantPolicies; }
    public void setTenantPolicies(Map<String, TenantPolicies> tenantPolicies) { this.tenantPolicies = tenantPolicies; }
    
    /**
     * Token limits configuration
     */
    public static class TokenLimits {
        private int maxTokensPerRequest = 10000;
        private int maxTokensPerLifetime = 100000;
        private int maxTokensPerTenant = 1000000;
        
        public int getMaxTokensPerRequest() { return maxTokensPerRequest; }
        public void setMaxTokensPerRequest(int maxTokensPerRequest) { this.maxTokensPerRequest = maxTokensPerRequest; }
        
        public int getMaxTokensPerLifetime() { return maxTokensPerLifetime; }
        public void setMaxTokensPerLifetime(int maxTokensPerLifetime) { this.maxTokensPerLifetime = maxTokensPerLifetime; }
        
        public int getMaxTokensPerTenant() { return maxTokensPerTenant; }
        public void setMaxTokensPerTenant(int maxTokensPerTenant) { this.maxTokensPerTenant = maxTokensPerTenant; }
    }
    
    /**
     * Cost thresholds configuration
     */
    public static class CostThresholds {
        private double maxCostPerRequest = 1.0;
        private double maxCostPerLifetime = 10.0;
        private double maxCostPerTenant = 100.0;
        
        public double getMaxCostPerRequest() { return maxCostPerRequest; }
        public void setMaxCostPerRequest(double maxCostPerRequest) { this.maxCostPerRequest = maxCostPerRequest; }
        
        public double getMaxCostPerLifetime() { return maxCostPerLifetime; }
        public void setMaxCostPerLifetime(double maxCostPerLifetime) { this.maxCostPerLifetime = maxCostPerLifetime; }
        
        public double getMaxCostPerTenant() { return maxCostPerTenant; }
        public void setMaxCostPerTenant(double maxCostPerTenant) { this.maxCostPerTenant = maxCostPerTenant; }
    }
    
    /**
     * Iteration caps configuration
     */
    public static class IterationCaps {
        private int maxIterationsPerLifetime = 100;
        private int maxIterationsPerTenant = 1000;
        
        public int getMaxIterationsPerLifetime() { return maxIterationsPerLifetime; }
        public void setMaxIterationsPerLifetime(int maxIterationsPerLifetime) { this.maxIterationsPerLifetime = maxIterationsPerLifetime; }
        
        public int getMaxIterationsPerTenant() { return maxIterationsPerTenant; }
        public void setMaxIterationsPerTenant(int maxIterationsPerTenant) { this.maxIterationsPerTenant = maxIterationsPerTenant; }
    }
    
    /**
     * Execution timeouts configuration
     */
    public static class ExecutionTimeouts {
        private long maxExecutionTimeSeconds = 300; // 5 minutes
        private long maxLifetimeTimeSeconds = 3600; // 1 hour
        
        public long getMaxExecutionTimeSeconds() { return maxExecutionTimeSeconds; }
        public void setMaxExecutionTimeSeconds(long maxExecutionTimeSeconds) { this.maxExecutionTimeSeconds = maxExecutionTimeSeconds; }
        
        public long getMaxLifetimeTimeSeconds() { return maxLifetimeTimeSeconds; }
        public void setMaxLifetimeTimeSeconds(long maxLifetimeTimeSeconds) { this.maxLifetimeTimeSeconds = maxLifetimeTimeSeconds; }
    }
    
    /**
     * Tenant-specific policies
     */
    public static class TenantPolicies {
        private TokenLimits tokenLimits;
        private CostThresholds costThresholds;
        private IterationCaps iterationCaps;
        private ExecutionTimeouts executionTimeouts;
        
        public TokenLimits getTokenLimits() { return tokenLimits; }
        public void setTokenLimits(TokenLimits tokenLimits) { this.tokenLimits = tokenLimits; }
        
        public CostThresholds getCostThresholds() { return costThresholds; }
        public void setCostThresholds(CostThresholds costThresholds) { this.costThresholds = costThresholds; }
        
        public IterationCaps getIterationCaps() { return iterationCaps; }
        public void setIterationCaps(IterationCaps iterationCaps) { this.iterationCaps = iterationCaps; }
        
        public ExecutionTimeouts getExecutionTimeouts() { return executionTimeouts; }
        public void setExecutionTimeouts(ExecutionTimeouts executionTimeouts) { this.executionTimeouts = executionTimeouts; }
    }
} 